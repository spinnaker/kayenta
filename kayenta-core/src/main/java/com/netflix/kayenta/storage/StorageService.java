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

package com.netflix.kayenta.storage;

import java.util.List;
import java.util.Map;

public interface StorageService {
  boolean servicesAccount(String accountName);
  <T> T loadObject(String accountName, ObjectType objectType, String objectKey) throws IllegalArgumentException;
  <T> void storeObject(String accountName, ObjectType objectType, String objectKey, T obj, String filename, boolean isAnUpdate);
  void deleteObject(String accountName, ObjectType objectType, String objectKey);
  List<Map<String, Object>> listObjectKeys(String accountName, ObjectType objectType, List<String> applications, boolean skipIndex);

  default <T> void storeObject(String accountName, ObjectType objectType, String objectKey, T obj) {
    storeObject(accountName, objectType, objectKey, obj, null, true);
  }

  default List<Map<String, Object>> listObjectKeys(String accountName, ObjectType objectType) {
    return listObjectKeys(accountName, objectType, null, false);
  }
}
