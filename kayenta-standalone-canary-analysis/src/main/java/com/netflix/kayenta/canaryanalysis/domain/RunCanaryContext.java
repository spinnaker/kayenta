/*
 * Copyright (c) 2018 Nike, inc.
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

package com.netflix.kayenta.canaryanalysis.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.netflix.kayenta.canary.CanaryClassifierThresholdsConfig;
import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.canary.CanaryScopePair;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

import java.util.Map;

/**
 * Internal model used in the canary analysis pipeline execution stages
 */
@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RunCanaryContext {

  private String application;
  private String parentPipelineExecutionId;
  private String canaryConfigId;
  private String user;
  private String metricsAccountName;
  private String storageAccountName;
  private Map<String, Object> siteLocal;
  @NonNull
  private CanaryConfig canaryConfig;
  @NonNull
  private Map<String, CanaryScopePair> scopes;
  @NonNull
  private CanaryClassifierThresholdsConfig scoreThresholds;
}
