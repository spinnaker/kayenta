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

package com.netflix.kayenta.google.config;

import com.netflix.kayenta.google.security.GoogleClientFactory;
import com.netflix.kayenta.google.security.GoogleJsonClientFactory;
import com.netflix.kayenta.google.security.GoogleNamedAccountCredentials;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnProperty("kayenta.google.enabled")
@ComponentScan({"com.netflix.kayenta.google"})
@Slf4j
public class GoogleConfiguration {

  @Bean
  @ConfigurationProperties("kayenta.google")
  GoogleConfigurationProperties googleConfigurationProperties() {
    return new GoogleConfigurationProperties();
  }

  @Bean
  boolean registerGoogleCredentials(
      GoogleConfigurationProperties googleConfigurationProperties,
      AccountCredentialsRepository accountCredentialsRepository)
      throws IOException {
    for (GoogleManagedAccount googleManagedAccount : googleConfigurationProperties.getAccounts()) {
      String name = googleManagedAccount.getName();
      String project = googleManagedAccount.getProject();
      List<AccountCredentials.Type> supportedTypes = googleManagedAccount.getSupportedTypes();

      log.info(
          "Registering Google account {} for project {} with supported types {}.",
          name,
          project,
          supportedTypes);

      try {
        String jsonKey = googleManagedAccount.getJsonKey();
        GoogleClientFactory googleClientFactory =
            StringUtils.hasLength(jsonKey)
                ? new GoogleJsonClientFactory(project, jsonKey)
                : new GoogleClientFactory(project);

        GoogleNamedAccountCredentials.GoogleNamedAccountCredentialsBuilder
            googleNamedAccountCredentialsBuilder =
                GoogleNamedAccountCredentials.builder()
                    .name(name)
                    .project(project)
                    .credentials(googleClientFactory);

        if (!CollectionUtils.isEmpty(supportedTypes)) {
          if (supportedTypes.contains(AccountCredentials.Type.METRICS_STORE)) {
            googleNamedAccountCredentialsBuilder.monitoring(googleClientFactory.getMonitoring());
          }

          if (supportedTypes.contains(AccountCredentials.Type.OBJECT_STORE)) {
            String bucket = googleManagedAccount.getBucket();
            String rootFolder = googleManagedAccount.getRootFolder();

            if (StringUtils.isEmpty(bucket)) {
              throw new IllegalArgumentException(
                  "Google/GCS account " + name + " is required to specify a bucket.");
            }

            if (StringUtils.isEmpty(rootFolder)) {
              throw new IllegalArgumentException(
                  "Google/GCS account " + name + " is required to specify a rootFolder.");
            }

            googleNamedAccountCredentialsBuilder.bucket(bucket);
            googleNamedAccountCredentialsBuilder.bucketLocation(
                googleManagedAccount.getBucketLocation());
            googleNamedAccountCredentialsBuilder.rootFolder(rootFolder);
            googleNamedAccountCredentialsBuilder.storage(googleClientFactory.getStorage());
          }

          googleNamedAccountCredentialsBuilder.supportedTypes(supportedTypes);
        }

        GoogleNamedAccountCredentials googleNamedAccountCredentials =
            googleNamedAccountCredentialsBuilder.build();
        accountCredentialsRepository.save(name, googleNamedAccountCredentials);
      } catch (Throwable t) {
        log.error("Could not load Google account " + name + ".", t);
      }
    }

    return true;
  }
}
