/*
 * Copyright 2023 Armory, Inc.
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

package com.netflix.kayenta.sql.migration;

import com.netflix.kayenta.sql.config.SqlProperties;
import com.netflix.kayenta.storage.ObjectType;
import com.netflix.kayenta.storage.StorageService;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@RequiredArgsConstructor
public class StorageDataMigrator {

  private final SqlProperties sqlProperties;
  private final StorageService sourceStorageService;
  private final StorageService targetStorageService;
  private final ExecutorService executorService;

  private void migrate(ObjectType objectType) {
    log.info("Migrating {}", objectType);

    var sourceAccountName = sqlProperties.getMigration().getSourceAccountName();
    var targetAccountName = sqlProperties.getMigration().getTargetAccountName();

    var sourceObjectKeys =
        sourceStorageService.listObjectKeys(sourceAccountName, objectType).stream()
            .map(sourceObjectKey -> (String) sourceObjectKey.get("id"))
            .collect(Collectors.toList());

    var targetObjectKeys =
        targetStorageService.listObjectKeys(targetAccountName, objectType).stream()
            .map(sourceObjectKey -> (String) sourceObjectKey.get("id"))
            .collect(Collectors.toList());

    var objectKeysToMigrate =
        sourceObjectKeys.stream()
            .filter(
                sourceObjectKey ->
                    targetObjectKeys.stream()
                        .filter(targetObjectKey -> Objects.equals(targetObjectKey, sourceObjectKey))
                        .findFirst()
                        .isEmpty())
            .collect(Collectors.toList());

    if (objectKeysToMigrate.isEmpty()) {
      log.info(
          "No objects to migrate for objectType: {}, sourceObjectCount: {}, targetObjectCount: {}",
          objectType,
          sourceObjectKeys.size(),
          targetObjectKeys.size());
      return;
    }

    for (var objectKey : objectKeysToMigrate) {
      executorService.submit(
          () -> {
            try {
              var object =
                  sourceStorageService.loadObject(sourceAccountName, objectType, objectKey);
              targetStorageService.storeObject(targetAccountName, objectType, objectKey, object);
            } catch (Exception e) {
              log.error(
                  "Unable to migrate objectType: {}, objectKey: {}", objectType, objectKey, e);
            }
          });
    }
  }

  @Scheduled(fixedDelay = 60000)
  public void migrate() {
    if (!sqlProperties.getMigration().isEnabled()) {
      log.info("Migration is disabled");
      return;
    }

    log.info("Migration started");
    migrate(ObjectType.CANARY_RESULT_ARCHIVE);
    migrate(ObjectType.CANARY_CONFIG);
    migrate(ObjectType.METRIC_SET_PAIR_LIST);
    migrate(ObjectType.METRIC_SET_LIST);
    log.info("Migration complete");
  }
}
