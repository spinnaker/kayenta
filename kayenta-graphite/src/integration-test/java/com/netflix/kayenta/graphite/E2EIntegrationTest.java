package com.netflix.kayenta.graphite;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.netflix.kayenta.Main;
import com.netflix.kayenta.canary.CanaryAdhocExecutionRequest;
import com.netflix.kayenta.canary.CanaryClassifierThresholdsConfig;
import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.canary.CanaryExecutionRequest;
import com.netflix.kayenta.canary.CanaryScope;
import com.netflix.kayenta.canary.CanaryScopePair;
import io.restassured.response.ValidatableResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.time.Instant;

import static com.netflix.kayenta.config.GraphiteIntegrationTestConfig.*;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.hamcrest.core.Is.is;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = Main.class
)

public class E2EIntegrationTest {

    public static final int CANARY_WINDOW_IN_MINUTES = 1;

    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private Instant metricsReportingStartTime;

    @LocalServerPort
    protected int serverPort;

    private String getUriTemplate() {
        return "http://localhost:" + serverPort + "%s";
    }

    @Test
    public void test_that_graphite_can_be_used_as_a_data_source_for_a_canary_execution_healthy() throws IOException {
        ValidatableResponse response = doCanaryExec(EXPERIMENT_SCOPE_HEALTHY);
        response.body("result.judgeResult.score.classification", is("Pass"));
    }

    @Test
    public void test_that_graphite_can_be_used_as_a_data_source_for_a_canary_execution_unhealthy() throws IOException {
        ValidatableResponse response = doCanaryExec(EXPERIMENT_SCOPE_UNHEALTHY);
        response.body("result.judgeResult.score.classification", is("Fail"));

    }

    private ValidatableResponse doCanaryExec(String scope) throws IOException {
        CanaryAdhocExecutionRequest request = new CanaryAdhocExecutionRequest();
        CanaryConfig canaryConfig = objectMapper.readValue(getClass().getClassLoader()
                .getResourceAsStream("integration-test-canary-config.json"), CanaryConfig.class);
        request.setCanaryConfig(canaryConfig);

        CanaryExecutionRequest executionRequest = new CanaryExecutionRequest();
        CanaryClassifierThresholdsConfig canaryClassifierThresholdsConfig = CanaryClassifierThresholdsConfig.builder()
                .marginal(50D).pass(75D).build();
        executionRequest.setThresholds(canaryClassifierThresholdsConfig);

        Instant end = metricsReportingStartTime.plus(CANARY_WINDOW_IN_MINUTES, MINUTES);

        CanaryScope control = new CanaryScope()
                .setScope(CONTROL_SCOPE_NAME)
                .setStart(metricsReportingStartTime)
                .setEnd(end);

        CanaryScope experiment = new CanaryScope()
                .setScope(scope)
                .setStart(metricsReportingStartTime)
                .setEnd(end);

        CanaryScopePair canaryScopePair = new CanaryScopePair();
        canaryScopePair.setControlScope(control);
        canaryScopePair.setExperimentScope(experiment);
        executionRequest.setScopes(ImmutableMap.of("default", canaryScopePair));
        request.setExecutionRequest(executionRequest);

        ValidatableResponse canaryExRes =
                given()
                        .contentType("application/json")
                        .queryParam("metricsAccountName", "my-graphite-account")
                        .queryParam("storageAccountName", "in-memory-store")
                        .body(request)
                        .when()
                        .post(String.format(getUriTemplate(), "/canary"))
                        .then()
                        .log().ifValidationFails()
                        .statusCode(200);

        String canaryExecutionId = canaryExRes.extract().body().jsonPath().getString("canaryExecutionId");
        ValidatableResponse response;
        do {
            response = when().get(String.format(getUriTemplate(), "/canary/" + canaryExecutionId))
                    .then().statusCode(200);
        } while (!response.extract().body().jsonPath().getBoolean("complete"));

        // verify the results are as expected
        return response.log().everything(true).body("status", is("succeeded"));
    }
}
