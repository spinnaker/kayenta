/*
 * Copyright 2018 Adobe
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

package com.netflix.kayenta.newrelic.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.kayenta.metrics.MetricsService;
import com.netflix.kayenta.newrelic.metrics.NewrelicMetricsService;
import com.netflix.kayenta.newrelic.security.NewrelicCredentials;
import com.netflix.kayenta.newrelic.security.NewrelicNamedAccountCredentials;
import com.netflix.kayenta.newrelic.service.NewrelicRemoteService;
import com.netflix.kayenta.retrofit.config.RetrofitClientFactory;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.squareup.okhttp.OkHttpClient;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import retrofit.converter.JacksonConverter;

@Configuration
@EnableConfigurationProperties
@ConditionalOnProperty("kayenta.newrelic.enabled")
@ComponentScan({"com.netflix.kayenta.newrelic"})
@Slf4j
public class NewrelicConfiguration {

  @Bean
  @ConfigurationProperties("kayenta.newrelic")
  NewrelicConfigurationProperties newrelicConfigurationProperties() {
    return new NewrelicConfigurationProperties();
  }

  @Bean
  @ConfigurationProperties("kayenta.newrelic.testControllerDefaults")
  NewrelicConfigurationTestControllerDefaultProperties newrelicConfigurationTestControllerDefaultProperties() {
    return new NewrelicConfigurationTestControllerDefaultProperties();
  }

  @Bean
  MetricsService newrelicMetricsService(
    NewrelicConfigurationProperties newrelicConfigurationProperties,
    RetrofitClientFactory retrofitClientFactory,
    ObjectMapper objectMapper,
    OkHttpClient okHttpClient,
    AccountCredentialsRepository accountCredentialsRepository) throws IOException {
    NewrelicMetricsService.NewrelicMetricsServiceBuilder metricsServiceBuilder =
      NewrelicMetricsService.builder();

    for (NewrelicManagedAccount account : newrelicConfigurationProperties.getAccounts()) {
      String name = account.getName();
      List<AccountCredentials.Type> supportedTypes = account.getSupportedTypes();

      NewrelicCredentials credentials = NewrelicCredentials.builder()
        .apiKey(account.getApiKey())
        .applicationKey(account.getApplicationKey())
        .build();

      NewrelicNamedAccountCredentials.NewrelicNamedAccountCredentialsBuilder accountCredentialsBuilder =
        NewrelicNamedAccountCredentials.builder()
          .name(name)
          .endpoint(account.getEndpoint())
          .credentials(credentials);

      if (!CollectionUtils.isEmpty(supportedTypes)) {
        if (supportedTypes.contains(AccountCredentials.Type.METRICS_STORE)) {
          accountCredentialsBuilder.newrelicRemoteService(retrofitClientFactory.createClient(
            NewrelicRemoteService.class,
            new JacksonConverter(objectMapper),
            account.getEndpoint(),
            okHttpClient
          ));
        }
        accountCredentialsBuilder.supportedTypes(supportedTypes);
      }

      accountCredentialsRepository.save(name, accountCredentialsBuilder.build());
      metricsServiceBuilder.accountName(name);
    }

    log.info("Populated NewrelicMetricsService with {} Newrelic accounts.",
      newrelicConfigurationProperties.getAccounts().size());
    return metricsServiceBuilder.build();
  }
}

