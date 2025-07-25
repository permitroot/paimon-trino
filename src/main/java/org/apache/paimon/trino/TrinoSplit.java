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

import org.apache.paimon.table.source.Split;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.trino.spi.HostAddress;
import io.trino.spi.SplitWeight;
import io.trino.spi.connector.ConnectorSplit;

import java.util.Collections;
import java.util.List;

/** Trino {@link ConnectorSplit}. */
public class TrinoSplit implements ConnectorSplit {

    private final String splitSerialized;

    private final Double weight;

    @JsonCreator
    public TrinoSplit(
            @JsonProperty("splitSerialized") String splitSerialized,
            @JsonProperty("weight") Double weight) {
        this.splitSerialized = splitSerialized;
        this.weight = weight;
    }

    public static TrinoSplit fromSplit(Split split, Double weight) {
        return new TrinoSplit(EncodingUtils.encodeObjectToString(split), weight);
    }

    public Split decodeSplit() {
        return EncodingUtils.decodeStringToObject(splitSerialized);
    }

    @JsonProperty
    public String getSplitSerialized() {
        return splitSerialized;
    }

    @JsonProperty
    public Double getWeight() {
        return weight;
    }

    @Override
    public boolean isRemotelyAccessible() {
        return true;
    }

    @Override
    public List<HostAddress> getAddresses() {
        return Collections.emptyList();
    }

    @Override
    public SplitWeight getSplitWeight() {
        return SplitWeight.fromProportion(weight);
    }
}
