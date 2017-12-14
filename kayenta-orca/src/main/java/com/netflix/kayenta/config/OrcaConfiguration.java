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

package com.netflix.kayenta.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.config.QueueConfiguration;
import com.netflix.spinnaker.orca.config.OrcaPersistenceConfiguration;
import com.netflix.spinnaker.orca.config.RedisConfiguration;
import com.netflix.spinnaker.orca.exceptions.DefaultExceptionHandler;
import com.netflix.spinnaker.orca.jackson.OrcaObjectMapper;
import com.netflix.spinnaker.orca.pipeline.RestrictExecutionDuringTimeWindow;
import com.netflix.spinnaker.orca.pipeline.persistence.jedis.JedisExecutionRepository;
import com.netflix.spinnaker.orca.pipeline.util.ContextParameterProcessor;
import com.netflix.spinnaker.orca.pipeline.util.StageNavigator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;

import javax.inject.Inject;
import java.net.InetAddress;
import java.util.UUID;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Configuration
@Import({
  JedisExecutionRepository.class,
  com.netflix.spinnaker.orca.config.OrcaConfiguration.class,
  OrcaPersistenceConfiguration.class,
  PropertyPlaceholderAutoConfiguration.class,
  QueueConfiguration.class,
  RedisConfiguration.class,
  RestrictExecutionDuringTimeWindow.class,
  StageNavigator.class,
})
@EnableConfigurationProperties
@ComponentScan({
  "com.netflix.kayenta.orca",
  "com.netflix.spinnaker.orca.pipeline",
})
@Slf4j
public class OrcaConfiguration {

  @Bean
  String currentInstanceId() {
    String hostname;

    try {
      hostname = InetAddress.getLocalHost().getHostName();
      log.info("Kayenta hostname is " + hostname);
    } catch (Exception e) {
      hostname = "UNKNOWN";
      log.warn("Failed to determine Kayenta hostname", e);
    }

    String currentInstanceId = UUID.randomUUID() + "@" + hostname;

    return currentInstanceId;
  }

  @Bean
  ContextParameterProcessor contextParameterProcessor() {
    return new ContextParameterProcessor();
  }

  @Bean
  DefaultExceptionHandler defaultExceptionHandler() {
    return new DefaultExceptionHandler();
  }
}
