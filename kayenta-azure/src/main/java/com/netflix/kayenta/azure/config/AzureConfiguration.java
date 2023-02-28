/*
 * Copyright 2019 Microsoft Corporation.
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

package com.netflix.kayenta.azure.config;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties
@ConditionalOnProperty("kayenta.azure.enabled")
@ComponentScan({"com.netflix.kayenta.azure"})
@Slf4j
public class AzureConfiguration {

  @Bean
  @ConfigurationProperties("kayenta.azure")
  AzureConfigurationProperties azureConfigurationProperties() {
    return new AzureConfigurationProperties();
  }

  @Bean
  boolean registerAzureCredentials(
      AzureConfigurationProperties azureConfigurationProperties,
      AccountCredentialsRepository accountCredentialsRepository) {

    List<AzureManagedAccount> azureAccounts = azureConfigurationProperties.getAccounts();

    for (AzureManagedAccount azureManagedAccount : azureAccounts) {
      String name = azureManagedAccount.getName();
      List<AccountCredentials.Type> supportedTypes = azureManagedAccount.getSupportedTypes();

      log.info("Registering Azure account {} with supported types {}.", name, supportedTypes);

      try {
        if (!CollectionUtils.isEmpty(supportedTypes)) {
          if (supportedTypes.contains(AccountCredentials.Type.OBJECT_STORE)) {
            String container = azureManagedAccount.getContainer();
            String rootFolder = azureManagedAccount.getRootFolder();

            if (StringUtils.isEmpty(container)) {
              throw new IllegalArgumentException(
                  "Azure/Blobs account " + name + " is required to specify a container.");
            }

            if (StringUtils.isEmpty(rootFolder)) {
              throw new IllegalArgumentException(
                  "Azure/Blobs account " + name + " is required to specify a rootFolder.");
            }
            azureManagedAccount.setAzureContainer(getAzureContainer(azureManagedAccount));
          }
        }
        accountCredentialsRepository.save(azureManagedAccount);
      } catch (Throwable t) {
        log.error("Could not load Azure account " + name + ".", t);
      }
    }

    return true;
  }

  public static CloudBlobContainer getAzureContainer(AzureManagedAccount account) throws Exception {
    final String storageConnectionString =
        "DefaultEndpointsProtocol=https;"
            + "AccountName="
            + account.getStorageAccountName()
            + ";"
            + "AccountKey="
            + account.getAccountAccessKey()
            + ";"
            + "EndpointSuffix="
            + account.getEndpointSuffix();
    // Retrieve storage account from connection-string.
    CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);

    // Create the blob client.
    CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

    // Get a reference to a container.
    // The container name must be lower case

    CloudBlobContainer container = blobClient.getContainerReference(account.getContainer());

    // Create the container if it does not exist.
    container.createIfNotExists();

    return container;
  }
}
