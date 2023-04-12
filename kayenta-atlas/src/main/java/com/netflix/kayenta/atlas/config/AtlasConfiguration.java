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

package com.netflix.kayenta.atlas.config;

import com.netflix.kayenta.atlas.backends.AtlasStorageUpdater;
import com.netflix.kayenta.atlas.backends.AtlasStorageUpdaterService;
import com.netflix.kayenta.atlas.backends.BackendUpdater;
import com.netflix.kayenta.atlas.backends.BackendUpdaterService;
import com.netflix.kayenta.atlas.metrics.AtlasMetricsService;
import com.netflix.kayenta.metrics.MetricsService;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty("kayenta.atlas.enabled")
@ComponentScan({"com.netflix.kayenta.atlas"})
@Slf4j
public class AtlasConfiguration {

  private final AtlasStorageUpdaterService atlasStorageUpdaterService;
  private final BackendUpdaterService backendUpdaterService;

  @Autowired
  public AtlasConfiguration(
      AtlasStorageUpdaterService atlasStorageUpdaterService,
      BackendUpdaterService backendUpdaterService) {
    this.atlasStorageUpdaterService = atlasStorageUpdaterService;
    this.backendUpdaterService = backendUpdaterService;
  }

  @Bean
  @ConfigurationProperties("kayenta.atlas")
  AtlasConfigurationProperties atlasConfigurationProperties() {
    return new AtlasConfigurationProperties();
  }

  @Bean
  MetricsService atlasMetricsService(
      AtlasConfigurationProperties atlasConfigurationProperties,
      AccountCredentialsRepository accountCredentialsRepository) {

    AtlasMetricsService.AtlasMetricsServiceBuilder atlasMetricsServiceBuilder =
        AtlasMetricsService.builder();
    for (AtlasManagedAccount atlasManagedAccount : atlasConfigurationProperties.getAccounts()) {
      String name = atlasManagedAccount.getName();
      List<AccountCredentials.Type> supportedTypes = atlasManagedAccount.getSupportedTypes();
      String backendsJsonUriPrefix = atlasManagedAccount.getBackendsJsonBaseUrl();

      log.info("Registering Atlas account {} with supported types {}.", name, supportedTypes);

      BackendUpdater backendUpdater = BackendUpdater.builder().uri(backendsJsonUriPrefix).build();
      AtlasStorageUpdater atlasStorageUpdater =
          AtlasStorageUpdater.builder().uri(backendsJsonUriPrefix).build();

      atlasManagedAccount.setAtlasStorageUpdater(atlasStorageUpdater);
      atlasManagedAccount.setBackendUpdater(backendUpdater);
      accountCredentialsRepository.save(atlasManagedAccount);

      backendUpdaterService.add(backendUpdater);
      atlasStorageUpdaterService.add(atlasStorageUpdater);
      atlasMetricsServiceBuilder.accountName(name);
    }

    AtlasMetricsService atlasMetricsService = atlasMetricsServiceBuilder.build();

    log.info(
        "Populated AtlasMetricsService with {} Atlas accounts.",
        atlasMetricsService.getAccountNames().size());

    return atlasMetricsService;
  }
}
