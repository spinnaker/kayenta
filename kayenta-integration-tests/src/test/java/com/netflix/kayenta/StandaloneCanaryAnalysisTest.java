/*
 * Copyright 2019 Playtika
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
package com.netflix.kayenta;

import static org.hamcrest.CoreMatchers.is;

import io.restassured.response.ValidatableResponse;
import org.junit.Test;

public class StandaloneCanaryAnalysisTest extends BaseIntegrationTest {

  @Test
  public void canaryAnalysisIsSuccessful() {
    String canaryAnalysisExecutionId = steps.createCanaryAnalysis("cpu-successful-analysis-case");

    ValidatableResponse response =
        steps.waitUntilCanaryAnalysisCompleted(canaryAnalysisExecutionId);

    response
        .body("executionStatus", is("SUCCEEDED"))
        .body("canaryAnalysisExecutionResult.hasWarnings", is(false))
        .body("canaryAnalysisExecutionResult.didPassThresholds", is(true))
        .body(
            "canaryAnalysisExecutionResult.canaryScoreMessage",
            is("Final canary score 100.0 met or exceeded the pass score threshold."));
  }

  @Test
  public void canaryAnalysisIsFailed() {
    String canaryAnalysisExecutionId = steps.createCanaryAnalysis("cpu-marginal-analysis-case");

    ValidatableResponse response =
        steps.waitUntilCanaryAnalysisCompleted(canaryAnalysisExecutionId);

    response
        .body("executionStatus", is("TERMINAL"))
        .body("canaryAnalysisExecutionResult.hasWarnings", is(false))
        .body("canaryAnalysisExecutionResult.didPassThresholds", is(false))
        .body(
            "canaryAnalysisExecutionResult.canaryScoreMessage",
            is("Final canary score 0.0 is not above the marginal score threshold."));
  }
}
