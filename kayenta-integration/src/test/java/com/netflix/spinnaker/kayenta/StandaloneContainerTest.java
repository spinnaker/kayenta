/*
 * Copyright 2024 Salesforce, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.spinnaker.kayenta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class StandaloneContainerTest {

  private static final String REDIS_NETWORK_ALIAS = "redisHost";

  private static final int REDIS_PORT = 6379;

  private static final Logger logger = LoggerFactory.getLogger(StandaloneContainerTest.class);

  private static final Network network = Network.newNetwork();

  private static final GenericContainer redis =
      new GenericContainer(DockerImageName.parse("library/redis:5-alpine"))
          .withNetwork(network)
          .withNetworkAliases(REDIS_NETWORK_ALIAS)
          .withExposedPorts(REDIS_PORT);

  private static GenericContainer kayentaContainer;

  @BeforeAll
  static void setupOnce() throws Exception {
    String fullDockerImageName = System.getenv("FULL_DOCKER_IMAGE_NAME");

    // Skip the tests if there's no docker image.  This allows gradlew build to work.
    assumeTrue(fullDockerImageName != null);

    redis.start();

    DockerImageName dockerImageName = DockerImageName.parse(fullDockerImageName);

    kayentaContainer =
        new GenericContainer(dockerImageName)
            .withNetwork(network)
            .withExposedPorts(8090)
            .dependsOn(redis)
            .waitingFor(Wait.forHealthcheck().withStartupTimeout(Duration.ofSeconds(90)))
            .withEnv("SPRING_APPLICATION_JSON", getSpringApplicationJson());

    Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(logger);
    kayentaContainer.start();
    kayentaContainer.followOutput(logConsumer);
  }

  private static String getSpringApplicationJson() throws JsonProcessingException {
    String redisUrl = "redis://" + REDIS_NETWORK_ALIAS + ":" + REDIS_PORT;
    logger.info("redisUrl: '{}'", redisUrl);
    Map<String, String> properties = Map.of("redis.connection", redisUrl);
    ObjectMapper mapper = new ObjectMapper();
    return mapper.writeValueAsString(properties);
  }

  @AfterAll
  static void cleanupOnce() {
    if (kayentaContainer != null) {
      kayentaContainer.stop();
    }

    if (redis != null) {
      redis.stop();
    }
  }

  @BeforeEach
  void init(TestInfo testInfo) {
    System.out.println("--------------- Test " + testInfo.getDisplayName());
  }

  @Test
  void testHealthCheck() throws Exception {
    // hit an arbitrary endpoint
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(
                new URI(
                    "http://"
                        + kayentaContainer.getHost()
                        + ":"
                        + kayentaContainer.getFirstMappedPort()
                        + "/health"))
            .GET()
            .build();

    HttpClient client = HttpClient.newHttpClient();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertThat(response).isNotNull();
    logger.info("response: {}, {}", response.statusCode(), response.body());
    assertThat(response.statusCode()).isEqualTo(200);
  }
}
