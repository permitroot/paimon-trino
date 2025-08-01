/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.paimon.trino;

import org.apache.paimon.utils.StringUtils;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import io.airlift.bootstrap.Bootstrap;
import io.airlift.json.JsonModule;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.trino.filesystem.manager.FileSystemModule;
import io.trino.plugin.base.classloader.ClassLoaderSafeConnectorMetadata;
import io.trino.plugin.base.classloader.ClassLoaderSafeConnectorPageSinkProvider;
import io.trino.plugin.base.classloader.ClassLoaderSafeConnectorPageSourceProvider;
import io.trino.plugin.base.classloader.ClassLoaderSafeConnectorSplitManager;
import io.trino.plugin.hive.NodeVersion;
import io.trino.plugin.hive.orc.OrcReaderConfig;
import io.trino.spi.classloader.ThreadContextClassLoader;
import io.trino.spi.connector.Connector;
import io.trino.spi.connector.ConnectorContext;
import io.trino.spi.connector.ConnectorFactory;
import io.trino.spi.function.FunctionProvider;
import io.trino.spi.function.table.ConnectorTableFunction;
import io.trino.spi.type.TypeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Trino {@link ConnectorFactory}. */
public class TrinoConnectorFactory implements ConnectorFactory {

    private static final Logger LOG = LoggerFactory.getLogger(TrinoConnectorFactory.class);

    // see https://trino.io/docs/current/connector/hive.html#hive-general-configuration-properties
    private static final String HADOOP_CONF_FILES_KEY = "hive.config.resources";
    // see org.apache.paimon.utils.HadoopUtils
    private static final String HADOOP_CONF_PREFIX = "hadoop.";

    @Override
    public String getName() {
        return "paimon";
    }

    @Override
    public Connector create(
            String catalogName, Map<String, String> config, ConnectorContext context) {
        return create(catalogName, config, context, new EmptyModule());
    }

    public Connector create(
            String catalogName,
            Map<String, String> config,
            ConnectorContext context,
            Module module) {
        config = new HashMap<>(config);
        if (config.containsKey(HADOOP_CONF_FILES_KEY)) {
            for (String hadoopXml : config.get(HADOOP_CONF_FILES_KEY).split(",")) {
                try {
                    readHadoopXml(hadoopXml, config);
                } catch (Exception e) {
                    LOG.warn(
                            "Failed to read hadoop xml file " + hadoopXml + ", skipping this file.",
                            e);
                }
            }
        }

        ClassLoader classLoader = TrinoConnectorFactory.class.getClassLoader();
        try (ThreadContextClassLoader ignored = new ThreadContextClassLoader(classLoader)) {
            Bootstrap app =
                    new Bootstrap(
                            new JsonModule(),
                            new TrinoModule(config),
                            // bind the trino file system module
                            newFileSystemModule(catalogName, context),
                            binder -> {
                                binder.bind(NodeVersion.class)
                                        .toInstance(
                                                new NodeVersion(
                                                        context.getNodeManager()
                                                                .getCurrentNode()
                                                                .getVersion()));
                                binder.bind(TypeManager.class).toInstance(context.getTypeManager());
                                binder.bind(OpenTelemetry.class)
                                        .toInstance(context.getOpenTelemetry());
                                binder.bind(Tracer.class).toInstance(context.getTracer());
                                binder.bind(OrcReaderConfig.class)
                                        .toInstance(new OrcReaderConfig());
                            },
                            module);

            Injector injector =
                    app.doNotInitializeLogging()
                            .setRequiredConfigurationProperties(Map.of())
                            .setOptionalConfigurationProperties(config)
                            .initialize();

            TrinoMetadata trinoMetadata = injector.getInstance(TrinoMetadataFactory.class).create();
            TrinoSplitManager trinoSplitManager = injector.getInstance(TrinoSplitManager.class);
            TrinoPageSourceProvider trinoPageSourceProvider =
                    injector.getInstance(TrinoPageSourceProvider.class);
            TrinoPageSinkProvider trinoPageSinkProvider =
                    injector.getInstance(TrinoPageSinkProvider.class);
            TrinoNodePartitioningProvider trinoNodePartitioningProvider =
                    injector.getInstance(TrinoNodePartitioningProvider.class);
            TrinoSessionProperties trinoSessionProperties =
                    injector.getInstance(TrinoSessionProperties.class);
            TrinoTableOptions trinoTableOptions = injector.getInstance(TrinoTableOptions.class);
            Set<ConnectorTableFunction> connectorTableFunctions =
                    injector.getInstance(new Key<>() {});
            FunctionProvider functionProvider = injector.getInstance(FunctionProvider.class);

            return new TrinoConnector(
                    new ClassLoaderSafeConnectorMetadata(trinoMetadata, classLoader),
                    new ClassLoaderSafeConnectorSplitManager(trinoSplitManager, classLoader),
                    new ClassLoaderSafeConnectorPageSourceProvider(
                            trinoPageSourceProvider, classLoader),
                    new ClassLoaderSafeConnectorPageSinkProvider(
                            trinoPageSinkProvider, classLoader),
                    trinoNodePartitioningProvider,
                    trinoTableOptions,
                    trinoSessionProperties,
                    connectorTableFunctions,
                    functionProvider);
        }
    }

    private static void readHadoopXml(String path, Map<String, String> config) throws Exception {
        path = path.trim();
        if (path.isEmpty()) {
            return;
        }

        File xmlFile = new File(path);
        NodeList propertyNodes =
                DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .parse(xmlFile)
                        .getElementsByTagName("property");
        for (int i = 0; i < propertyNodes.getLength(); i++) {
            Node propertyNode = propertyNodes.item(i);
            if (propertyNode.getNodeType() == 1) {
                Element propertyElement = (Element) propertyNode;
                String key = propertyElement.getElementsByTagName("name").item(0).getTextContent();
                String value =
                        propertyElement.getElementsByTagName("value").item(0).getTextContent();
                if (!StringUtils.isNullOrWhitespaceOnly(value)) {
                    config.putIfAbsent(HADOOP_CONF_PREFIX + key, value);
                }
            }
        }
    }

    /** Empty module for paimon connector factory. */
    public static class EmptyModule implements Module {
        @Override
        public void configure(Binder binder) {}
    }

    private static FileSystemModule newFileSystemModule(
            String catalogName, ConnectorContext context) {
        Constructor<?> constructor = FileSystemModule.class.getConstructors()[0];
        try {
            if (constructor.getParameterCount() == 0) {
                return (FileSystemModule) constructor.newInstance();
            } else if (constructor.getParameterCount() == 3) {
                // for trino 440
                return (FileSystemModule)
                        constructor.newInstance(
                                catalogName, context.getNodeManager(), context.getOpenTelemetry());
            } else if (constructor.getParameterCount() == 4) {
                // for trino 476
                return (FileSystemModule)
                        constructor.newInstance(
                                catalogName,
                                context.getNodeManager(),
                                context.getOpenTelemetry(),
                                false);
            } else {
                throw new RuntimeException("Unsupported trino version");
            }
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
