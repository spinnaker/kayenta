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
 *
 */

package com.netflix.kayenta.signalfx.config;

import com.netflix.kayenta.metrics.MetricsService;
import com.netflix.kayenta.retrofit.config.RetrofitClientFactory;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.kayenta.signalfx.metrics.SignalFxMetricsService;
import com.netflix.kayenta.signalfx.service.SignalFxConverter;
import com.netflix.kayenta.signalfx.service.SignalFxSignalFlowRemoteService;
import com.squareup.okhttp.OkHttpClient;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty("kayenta.signalfx.enabled")
@ComponentScan({"com.netflix.kayenta.signalfx"})
@Slf4j
public class SignalFxConfiguration {

  @Bean
  @ConfigurationProperties("kayenta.signalfx")
  SignalFxConfigurationProperties signalFxConfigurationProperties() {
    return new SignalFxConfigurationProperties();
  }

  @Bean
  Map<String, SignalFxScopeConfiguration> signalFxScopeConfigurationMap(
      SignalFxConfigurationProperties signalFxConfigurationProperties) {
    return signalFxConfigurationProperties.getAccounts().stream()
        .collect(
            Collectors.toMap(
                SignalFxManagedAccount::getName,
                accountConfig ->
                    SignalFxScopeConfiguration.builder()
                        .defaultScopeKey(accountConfig.getDefaultScopeKey())
                        .defaultLocationKey(accountConfig.getDefaultLocationKey())
                        .build()));
  }

  @Bean
  MetricsService signalFxMetricService(
      SignalFxConfigurationProperties signalFxConfigurationProperties,
      RetrofitClientFactory retrofitClientFactory,
      OkHttpClient okHttpClient,
      AccountCredentialsRepository accountCredentialsRepository) {

    SignalFxMetricsService.SignalFxMetricsServiceBuilder metricsServiceBuilder =
        SignalFxMetricsService.builder();

    signalFxConfigurationProperties.getAccounts().parallelStream()
        .forEach(
            signalFxManagedAccount -> {
              signalFxManagedAccount.setSignalFlowService(
                  retrofitClientFactory.createClient(
                      SignalFxSignalFlowRemoteService.class,
                      new SignalFxConverter(),
                      signalFxManagedAccount.getEndpoint(),
                      okHttpClient));

              accountCredentialsRepository.save(signalFxManagedAccount);
              metricsServiceBuilder.accountName(signalFxManagedAccount.getName());
            });

    log.info(
        "Configured the SignalFx Metrics Service with the following accounts: {}",
        String.join(
            ",",
            signalFxConfigurationProperties.getAccounts().stream()
                .map(SignalFxManagedAccount::getName)
                .collect(Collectors.toList())));

    return metricsServiceBuilder.build();
  }
}
