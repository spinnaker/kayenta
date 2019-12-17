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

package com.netflix.kayenta.oss.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.kayenta.alicloud.security.AliCloudNamedAccountCredentials;
import com.netflix.kayenta.oss.storage.OssStorageService;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
@ConditionalOnProperty("kayenta.oss.enabled")
@ComponentScan({"com.netflix.kayenta.oss"})
@Slf4j
public class OssConfiguration {

  @Autowired ObjectMapper kayentaObjectMapper;

  @Bean
  @DependsOn({"registerAlicloudCredentials"})
  public OssStorageService ossStorageService(
      AccountCredentialsRepository accountCredentialsRepository) {
    OssStorageService.OssStorageServiceBuilder ossStorageServiceBuilder =
        OssStorageService.builder();

    accountCredentialsRepository.getAll().stream()
        .filter(c -> c instanceof AliCloudNamedAccountCredentials)
        .filter(c -> c.getSupportedTypes().contains(AccountCredentials.Type.OBJECT_STORE))
        .map(c -> c.getName())
        .forEach(ossStorageServiceBuilder::accountName);

    OssStorageService ossStorageService =
        ossStorageServiceBuilder.objectMapper(kayentaObjectMapper).build();

    log.info(
        "Populated OSSStorageService with {} alicloud accounts.",
        ossStorageService.getAccountNames().size());

    return ossStorageService;
  }
}
