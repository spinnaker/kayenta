/*
 * Copyright 2018 Netflix, Inc.
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
package com.netflix.kayenta.atlas.backends;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.kayenta.atlas.model.Backend;
import com.netflix.kayenta.atlas.service.BackendsRemoteService;
import com.netflix.kayenta.retrofit.config.RemoteService;
import com.netflix.kayenta.retrofit.config.RetrofitClientFactory;
import com.netflix.spinnaker.security.AuthenticatedRequest;
import com.squareup.okhttp.OkHttpClient;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import retrofit.RetrofitError;
import retrofit.converter.JacksonConverter;

@Slf4j
@Builder
public class BackendUpdater {
  @Getter private final BackendDatabase backendDatabase = new BackendDatabase();

  @NotNull private String uri;

  // If we have retrieved backends.json at least once, we will keep using it forever
  // even if we fail later.  It doesn't really change much over time, so this
  // is likely safe enough.
  @Builder.Default private boolean succeededAtLeastOnce = false;

  boolean run(
      RetrofitClientFactory retrofitClientFactory,
      ObjectMapper objectMapper,
      OkHttpClient okHttpClient) {
    RemoteService remoteService = new RemoteService();
    remoteService.setBaseUrl(uri);
    BackendsRemoteService backendsRemoteService =
        retrofitClientFactory.createClient(
            BackendsRemoteService.class,
            new JacksonConverter(objectMapper),
            remoteService,
            okHttpClient);
    try {
      List<Backend> backends = AuthenticatedRequest.allowAnonymous(backendsRemoteService::fetch);
      backendDatabase.update(backends);
    } catch (RetrofitError e) {
      log.warn("While fetching atlas backends from " + uri, e);
      return succeededAtLeastOnce;
    }
    succeededAtLeastOnce = true;
    return true;
  }
}
