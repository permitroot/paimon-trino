# Development
## Build
### maven
```
mvn clean install -DskipTests
```

```
docker build -t paimon-trino-builder .
docker run -it --rm -v .:/paimon-trino:rw --privileged --network=host paimon-trino-builder mvn clean install -DskipTests
```

## Issues
### issue
```
[INFO] There are 13 errors reported by Checkstyle 8.14 with /tools/maven/checkstyle.xml ruleset.
[ERROR] src/main/java/org/apache/paimon/trino/TrinoSplit.java:[26] (imports) ImportOrder: Import org.apache.paimon.table.source.Split appears after other imports that it should precede
[ERROR] src/main/java/org/apache/paimon/trino/TrinoSplit.java:[75] (whitespace) EmptyLineSeparator: 'METHOD_DEF' has more than 1 empty lines before.
[ERROR] src/main/java/org/apache/paimon/trino/TrinoMetadataFactory.java:[24] (imports) ImportOrder: Import org.apache.paimon.options.Options appears after other imports that it should precede
[ERROR] src/main/java/org/apache/paimon/trino/TrinoPageSourceProvider.java:[47] (imports) ImportOrder: Import org.apache.paimon.CoreOptions appears after other imports that it should precede
[ERROR] src/main/java/org/apache/paimon/trino/TrinoPageSourceProvider.java:[63] (imports) ImportOrder: 'org.joda.time.DateTimeZone' should be separated from previous imports.
[ERROR] src/main/java/org/apache/paimon/trino/catalog/TrinoCatalog.java:[26] (imports) ImportOrder: Import org.apache.paimon.catalog.Catalog appears after other imports that it should precede
[ERROR] src/main/java/org/apache/paimon/trino/catalog/TrinoCatalog.java:[41] (imports) ImportOrder: 'org.slf4j.Logger' should be separated from previous imports.
[ERROR] src/main/java/org/apache/paimon/trino/TrinoConnectorFactory.java:[43] (imports) ImportOrder: Import org.apache.paimon.utils.StringUtils appears after other imports that it should precede
[ERROR] src/main/java/org/apache/paimon/trino/TrinoConnectorFactory.java:[44] (imports) ImportOrder: 'org.slf4j.Logger' should be separated from previous imports.
[ERROR] src/main/java/org/apache/paimon/trino/TrinoMetadata.java:[56] (imports) ImportOrder: Import org.apache.paimon.CoreOptions appears after other imports that it should precede
[ERROR] src/main/java/org/apache/paimon/trino/TrinoMetadata.java:[422] (whitespace) EmptyLineSeparator: 'METHOD_DEF' has more than 1 empty lines before.
[ERROR] src/test/java/org/apache/paimon/trino/TrinoDistributedQueryTest.java:[23] (imports) ImportOrder: Import org.apache.paimon.shade.guava30.com.google.common.collect.ImmutableMap appears after other imports that it should precede
[ERROR] src/test/java/org/apache/paimon/trino/TrinoDistributedQueryTest.java:[544] (whitespace) EmptyLineSeparator: 'METHOD_DEF' has more than 1 empty lines before.
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
```

## resolved
```
mvn spotless:apply
```

### issue
```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.8.0:compile (default-compile) on project paimon-trino-476: Compilation failure: Compilation failure: 
[ERROR] /Users/user/git/permitroot/paimon-trino/src/main/java/org/apache/paimon/trino/catalog/TrinoCatalog.java:[49,8] org.apache.paimon.trino.catalog.TrinoCatalog is not abstract and does not override abstract method listPartitions(org.apache.paimon.catalog.Identifier) in org.apache.paimon.catalog.Catalog
[ERROR] /Users/user/git/permitroot/paimon-trino/src/main/java/org/apache/paimon/trino/catalog/TrinoCatalog.java:[225,33] listPartitions(org.apache.paimon.catalog.Identifier) in org.apache.paimon.trino.catalog.TrinoCatalog cannot implement listPartitions(org.apache.paimon.catalog.Identifier) in org.apache.paimon.catalog.Catalog
[ERROR]   return type java.util.List<org.apache.paimon.manifest.PartitionEntry> is not compatible with java.util.List<org.apache.paimon.partition.Partition>
[ERROR] /Users/user/git/permitroot/paimon-trino/src/main/java/org/apache/paimon/trino/catalog/TrinoCatalog.java:[224,5] method does not override or implement a method from a supertype
[ERROR] /Users/user/git/permitroot/paimon-trino/src/main/java/org/apache/paimon/trino/catalog/TrinoCatalog.java:[227,38] incompatible types: java.util.List<org.apache.paimon.partition.Partition> cannot be converted to java.util.List<org.apache.paimon.manifest.PartitionEntry>
[ERROR] -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[
```


# Apache Paimon Trino Connector

This repository is Trino Connector for the [Apache Paimon](https://paimon.apache.org/) project.

## About

Apache Paimon is an open source project of [The Apache Software Foundation](https://apache.org/) (ASF).
