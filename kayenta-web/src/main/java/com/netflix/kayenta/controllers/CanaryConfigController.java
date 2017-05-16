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

package com.netflix.kayenta.controllers;

import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.kayenta.storage.ObjectType;
import com.netflix.kayenta.storage.StorageService;
import com.netflix.kayenta.storage.StorageServiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/canaryConfig")
@Slf4j
public class CanaryConfigController {

  private static Pattern canaryConfigIdPattern = Pattern.compile("[a-z,0-9,\\-,\\_]*");

  @Autowired
  AccountCredentialsRepository accountCredentialsRepository;

  @Autowired
  StorageServiceRepository storageServiceRepository;

  @RequestMapping(value = "/{canaryConfigId:.+}", method = RequestMethod.GET)
  public CanaryConfig loadCanaryConfig(@RequestParam(required = false) final String accountName,
                                       @PathVariable String canaryConfigId) {
    String resolvedAccountName = CredentialsHelper.resolveAccountByNameOrType(accountName,
                                                                              AccountCredentials.Type.OBJECT_STORE,
                                                                              accountCredentialsRepository);
    Optional<StorageService> storageService = storageServiceRepository.getOne(resolvedAccountName);

    canaryConfigId = canaryConfigId.toLowerCase();

    if (storageService.isPresent()) {
      return storageService.get().loadObject(resolvedAccountName, ObjectType.CANARY_CONFIG, canaryConfigId);
    } else {
      log.debug("No storage service was configured; skipping placeholder logic to read from bucket.");
      return null;
    }
  }

  @RequestMapping(consumes = "application/context+json", method = RequestMethod.POST)
  public String storeCanaryConfig(@RequestParam(required = false) final String accountName,
                                  @RequestBody CanaryConfig canaryConfig) throws IOException {
    String resolvedAccountName = CredentialsHelper.resolveAccountByNameOrType(accountName,
                                                                              AccountCredentials.Type.OBJECT_STORE,
                                                                              accountCredentialsRepository);
    Optional<StorageService> storageService = storageServiceRepository.getOne(resolvedAccountName);

    if (StringUtils.isEmpty(canaryConfig.getName())) {
      canaryConfig.setName(UUID.randomUUID() + "");
    }

    if (storageService.isPresent()) {
      String canaryConfigId = canaryConfig.getName().toLowerCase();

      if (!canaryConfigIdPattern.matcher(canaryConfigId).matches()) {
        throw new IllegalArgumentException("Canary config cannot be named '" + canaryConfigId +
          "'. Names must contain only lowercase letters, numbers, dashes (-) and underscores (_).");
      }

      try {
        storageService.get().loadObject(resolvedAccountName, ObjectType.CANARY_CONFIG, canaryConfigId);
      } catch (IllegalArgumentException e) {
        storageService.get().storeObject(resolvedAccountName, ObjectType.CANARY_CONFIG, canaryConfigId, canaryConfig);

        return canaryConfigId;
      }

      throw new IllegalArgumentException("Canary config '" + canaryConfigId + "' already exists.");
    } else {
      log.debug("No storage service was configured; skipping placeholder logic to write to bucket.");

      return null;
    }
  }
}
