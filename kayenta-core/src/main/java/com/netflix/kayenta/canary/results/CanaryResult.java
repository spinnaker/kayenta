/*
 * Copyright 2017 Netflix, Inc.
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

package com.netflix.kayenta.canary.results;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.canary.CanaryExecutionRequest;
import lombok.*;

import java.time.Duration;

@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CanaryResult {

  @Getter
  CanaryJudgeResult judgeResult;

  @NonNull
  @Getter
  CanaryConfig config;

  @NonNull
  @Getter
  CanaryExecutionRequest canaryExecutionRequest;

  @Getter
  String metricSetPairListId;

  @Getter
  Duration canaryDuration;

  @NonNull
  @Getter
  String pipelineId;
}
