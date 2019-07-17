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

package com.netflix.kayenta.memory.config;

import com.netflix.kayenta.memory.security.MemoryAccountCredentials;
import com.netflix.kayenta.memory.security.MemoryNamedAccountCredentials;
import com.netflix.kayenta.memory.storage.MemoryStorageService;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.kayenta.storage.ObjectType;
import com.netflix.kayenta.storage.StorageService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

@Configuration
@ConditionalOnProperty("kayenta.memory.enabled")
@ComponentScan({"com.netflix.kayenta.memory"})
@Slf4j
public class MemoryConfiguration {

  @Bean
  @ConfigurationProperties("kayenta.memory")
  MemoryConfigurationProperties memoryConfigurationProperties() {
    return new MemoryConfigurationProperties();
  }

  @Bean
  StorageService storageService(
      MemoryConfigurationProperties memoryConfigurationProperties,
      AccountCredentialsRepository accountCredentialsRepository) {
    MemoryStorageService.MemoryStorageServiceBuilder memoryStorageServiceBuilder =
        MemoryStorageService.builder();

    for (MemoryManagedAccount memoryManagedAccount : memoryConfigurationProperties.getAccounts()) {
      String name = memoryManagedAccount.getName();
      List<AccountCredentials.Type> supportedTypes = memoryManagedAccount.getSupportedTypes();

      log.info("Registering Memory account {} with supported types {}.", name, supportedTypes);

      MemoryAccountCredentials memoryAccountCredentials =
          MemoryAccountCredentials.builder().build();
      MemoryNamedAccountCredentials.MemoryNamedAccountCredentialsBuilder
          memoryNamedAccountCredentialsBuilder =
              MemoryNamedAccountCredentials.builder()
                  .name(name)
                  .credentials(memoryAccountCredentials);

      if (!CollectionUtils.isEmpty(supportedTypes)) {
        memoryNamedAccountCredentialsBuilder.supportedTypes(supportedTypes);
      }

      // Set up the data maps for this in-memory storage account
      Map<ObjectType, Map<String, Object>> objectStorage = new ConcurrentHashMap<>();
      memoryNamedAccountCredentialsBuilder.objects(objectStorage);
      Map<ObjectType, Map<String, Map<String, Object>>> metadataStorage = new ConcurrentHashMap<>();
      memoryNamedAccountCredentialsBuilder.metadata(metadataStorage);

      MemoryNamedAccountCredentials memoryNamedAccountCredentials =
          memoryNamedAccountCredentialsBuilder.build();
      accountCredentialsRepository.save(name, memoryNamedAccountCredentials);
      memoryStorageServiceBuilder.accountName(name);
    }

    MemoryStorageService memoryStorageService = memoryStorageServiceBuilder.build();

    log.info(
        "Populated MemoryStorageService with {} in-memory accounts.",
        memoryStorageService.getAccountNames().size());

    return memoryStorageService;
  }
}
