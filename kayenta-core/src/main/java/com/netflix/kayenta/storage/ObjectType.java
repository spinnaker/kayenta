/*
 * Copyright 2017 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.kayenta.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.canary.CanaryExecutionStatusResponse;
import com.netflix.kayenta.domain.standalonecanaryanalysis.CanaryAnalysisExecutionStatusResponse;
import com.netflix.kayenta.metrics.MetricSet;
import com.netflix.kayenta.metrics.MetricSetPair;
import java.util.List;

public interface ObjectType {

  ObjectType CANARY_CONFIG =
      new StandardObjectType(
          new TypeReference<CanaryConfig>() {}, "canary_config", "canary_config.json");

  ObjectType CANARY_RESULT_ARCHIVE =
      new StandardObjectType(
          new TypeReference<CanaryExecutionStatusResponse>() {},
          "canary_archive",
          "canary_archive.json");

  ObjectType METRIC_SET_LIST =
      new StandardObjectType(
          new TypeReference<List<MetricSet>>() {}, "metrics", "metric_sets.json");

  ObjectType METRIC_SET_PAIR_LIST =
      new StandardObjectType(
          new TypeReference<List<MetricSetPair>>() {}, "metric_pairs", "metric_set_pairs.json");

  ObjectType STANDALONE_CANARY_RESULT_ARCHIVE =
      new StandardObjectType(
          new TypeReference<CanaryAnalysisExecutionStatusResponse>() {},
          "standalone_canary_archive",
          "standalone_canary_archive.json");

  TypeReference<?> getTypeReference();

  String getGroup();

  String getDefaultFilename();
}
