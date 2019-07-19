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

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;

import io.restassured.RestAssured;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

public class ManagementTest extends BaseIntegrationTest {

  @Value("${embedded.prometheus.port}")
  int prometheusPort;

  @Test
  public void prometheusTargetsAreAllReportingUp() {
    await()
        .atMost(30, TimeUnit.SECONDS)
        .pollInterval(500, TimeUnit.MILLISECONDS)
        .untilAsserted(
            () ->
                RestAssured.given()
                    .port(prometheusPort)
                    .get("/api/v1/targets")
                    .prettyPeek()
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .body("status", is("success"))
                    .body("data.activeTargets[0].health", is("up")));
  }

  @Test
  public void healthIsUp() {
    await()
        .atMost(30, TimeUnit.SECONDS)
        .pollInterval(500, TimeUnit.MILLISECONDS)
        .untilAsserted(
            () ->
                RestAssured.given()
                    .port(managementPort)
                    .get("/health")
                    .prettyPeek()
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .body("status", is("UP"))
                    .body("details.canaryConfigIndexingAgent.status", is("UP"))
                    .body("details.redisHealth.status", is("UP")));
  }
}