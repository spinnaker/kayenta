/*
 * Copyright 2018 Adobe
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

package com.netflix.kayenta.newrelic.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.netflix.kayenta.newrelic.service.NewRelicRemoteService;
import com.netflix.kayenta.retrofit.config.RemoteService;
import com.netflix.kayenta.security.AccountCredentials;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class NewRelicManagedAccount extends AccountCredentials<NewRelicManagedAccount> {
  private static final List<Type> SUPPORTED_TYPES =
      Collections.singletonList(AccountCredentials.Type.METRICS_STORE);
  private String apiKey;
  private String applicationKey;

  @NotNull @Builder.Default
  private RemoteService endpoint =
      new RemoteService().setBaseUrl("https://insights-api.newrelic.com");

  @Nullable private String defaultScopeKey;

  @Nullable private String defaultLocationKey;

  @Override
  public List<AccountCredentials.Type> getSupportedTypes() {
    return SUPPORTED_TYPES;
  }

  @Override
  public String getType() {
    return "newrelic";
  }

  @JsonIgnore private transient NewRelicRemoteService newRelicRemoteService;
}
