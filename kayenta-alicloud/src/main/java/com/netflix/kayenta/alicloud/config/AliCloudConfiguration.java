/*
 * Copyright 2019 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package com.netflix.kayenta.alicloud.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.netflix.kayenta.alicloud.security.AliCloudCredentials;
import com.netflix.kayenta.alicloud.security.AliCloudNamedAccountCredentials;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
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
@ConditionalOnProperty("kayenta.alicloud.enabled")
@ComponentScan({"com.netflix.kayenta.alicloud"})
@Slf4j
public class AliCloudConfiguration {

  @Bean
  @ConfigurationProperties("kayenta.alicloud")
  AliCloudConfigurationProperties alicloudConfigurationProperties() {
    return new AliCloudConfigurationProperties();
  }

  @Bean
  boolean registerAlicloudCredentials(
      AliCloudConfigurationProperties alicloudConfigurationProperties,
      AccountCredentialsRepository accountCredentialsRepository) {
    for (AliCloudManagedAccount alicloudManagedAccount :
        alicloudConfigurationProperties.getAccounts()) {
      String name = alicloudManagedAccount.getName();
      List<AccountCredentials.Type> supportedTypes = alicloudManagedAccount.getSupportedTypes();

      log.info("Registering alicloud account {} with supported types {}.", name, supportedTypes);
      OSS ossClient =
          new OSSClientBuilder()
              .build(
                  alicloudManagedAccount.getEndpoint(),
                  alicloudManagedAccount.getAccessKeyId(),
                  alicloudManagedAccount.getSecretAccessKey());

      try {
        AliCloudCredentials alicloudCredentials = new AliCloudCredentials();
        AliCloudNamedAccountCredentials.AliCloudNamedAccountCredentialsBuilder
            alicloudNamedAccountCredentialsBuilder =
                AliCloudNamedAccountCredentials.builder()
                    .name(name)
                    .credentials(alicloudCredentials);

        if (!CollectionUtils.isEmpty(supportedTypes)) {
          if (supportedTypes.contains(AccountCredentials.Type.OBJECT_STORE)) {
            String bucket = alicloudManagedAccount.getBucket();
            String rootFolder = alicloudManagedAccount.getRootFolder();

            if (StringUtils.isEmpty(bucket)) {
              throw new IllegalArgumentException(
                  "alicloud/oss account " + name + " is required to specify a bucket.");
            }

            if (StringUtils.isEmpty(rootFolder)) {
              throw new IllegalArgumentException(
                  "alicloud/oss account " + name + " is required to specify a rootFolder.");
            }

            alicloudNamedAccountCredentialsBuilder.bucket(bucket);
            alicloudNamedAccountCredentialsBuilder.rootFolder(rootFolder);
            alicloudNamedAccountCredentialsBuilder.ossClient(ossClient);
          }

          alicloudNamedAccountCredentialsBuilder.supportedTypes(supportedTypes);
        }

        AliCloudNamedAccountCredentials alicloudNamedAccountCredentials =
            alicloudNamedAccountCredentialsBuilder.build();
        accountCredentialsRepository.save(name, alicloudNamedAccountCredentials);
      } catch (Throwable t) {
        log.error("Could not load alicloud account " + name + ".", t);
      }
    }

    return true;
  }
}
