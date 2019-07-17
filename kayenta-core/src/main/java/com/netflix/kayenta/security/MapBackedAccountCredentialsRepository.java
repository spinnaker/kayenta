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

package com.netflix.kayenta.security;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MapBackedAccountCredentialsRepository implements AccountCredentialsRepository {

  private final Map<String, AccountCredentials> accountNameToCredentialsMap =
      new ConcurrentHashMap<>();

  @Override
  public Optional<AccountCredentials> getOne(String accountName) {
    return Optional.ofNullable(accountNameToCredentialsMap.get(accountName));
  }

  @Override
  public Optional<AccountCredentials> getOne(AccountCredentials.Type credentialsType) {
    return accountNameToCredentialsMap.values().stream()
        .filter(a -> a.getSupportedTypes().contains(credentialsType))
        .findFirst();
  }

  @Override
  public Set<AccountCredentials> getAll() {
    return new HashSet<>(accountNameToCredentialsMap.values());
  }

  @Override
  public AccountCredentials save(String name, AccountCredentials credentials) {
    return accountNameToCredentialsMap.put(credentials.getName(), credentials);
  }
}
