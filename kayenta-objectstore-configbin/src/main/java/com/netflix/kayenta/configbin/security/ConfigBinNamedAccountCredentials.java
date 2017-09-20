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

package com.netflix.kayenta.configbin.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.netflix.kayenta.configbin.service.ConfigBinRemoteService;
import com.netflix.kayenta.retrofit.config.RemoteService;
import com.netflix.kayenta.security.AccountCredentials;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@Builder
@Data
public class ConfigBinNamedAccountCredentials implements AccountCredentials<ConfigBinAccountCredentials> {

  @NotNull
  private String name;

  @NotNull
  @Singular
  private List<Type> supportedTypes;

  @NotNull
  private ConfigBinAccountCredentials credentials;

  @NotNull
  @Getter
  @Setter
  private RemoteService endpoint;

  @NotNull
  private String ownerApp;

  @NotNull
  private String configType;

  @Override
  public String getType() { return "configbin"; }

  @JsonIgnore
  private ConfigBinRemoteService remoteService;
}
