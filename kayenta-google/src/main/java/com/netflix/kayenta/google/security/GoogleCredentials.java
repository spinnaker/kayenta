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

package com.netflix.kayenta.google.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.monitoring.v3.Monitoring;
import com.google.api.services.monitoring.v3.MonitoringScopes;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

@ToString
@Slf4j
public class GoogleCredentials {

  private static String applicationVersion =
    Optional.ofNullable(GoogleCredentials.class.getPackage().getImplementationVersion()).orElse("Unknown");

  @Getter
  private String project;

  public GoogleCredentials(String project) {
    this.project = project;
  }

  public Monitoring getMonitoring() throws IOException {
    JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
    HttpTransport httpTransport = buildHttpTransport();
    GoogleCredential credential = getCredential(httpTransport, jsonFactory, MonitoringScopes.all());
    HttpRequestInitializer reqInit = setHttpTimeout(credential);
    String applicationName = "Spinnaker/" + applicationVersion;

    return new Monitoring.Builder(httpTransport, jsonFactory, credential)
      .setApplicationName(applicationName)
      .setHttpRequestInitializer(reqInit)
      .build();
  }

  public Storage getStorage() throws IOException {
    JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
    HttpTransport httpTransport = buildHttpTransport();
    GoogleCredential credential = getCredential(httpTransport, jsonFactory, StorageScopes.all());
    HttpRequestInitializer reqInit = setHttpTimeout(credential);
    String applicationName = "Spinnaker/" + applicationVersion;

    return new Storage.Builder(httpTransport, jsonFactory, credential)
      .setApplicationName(applicationName)
      .setHttpRequestInitializer(reqInit)
      .build();
  }

  protected GoogleCredential getCredential(HttpTransport httpTransport, JsonFactory jsonFactory, Collection<String> scopes) throws IOException {
    log.debug("Loading credentials for project {} using application default credentials, with scopes {}.", project, scopes);

    // No JSON key was specified in matching config on key server, so use application default credentials.
    return GoogleCredential.getApplicationDefault().createScoped(scopes);
  }

  protected HttpTransport buildHttpTransport() {
    try {
      return GoogleNetHttpTransport.newTrustedTransport();
    } catch (Exception e) {
      throw new RuntimeException("Failed to create trusted transport", e);
    }
  }

  static HttpRequestInitializer setHttpTimeout(final HttpRequestInitializer requestInitializer) {
    return new HttpRequestInitializer() {
      @Override
      public void initialize(HttpRequest httpRequest) throws IOException {
        requestInitializer.initialize(httpRequest);
        httpRequest.setConnectTimeout(2 * 60000);  // 2 minutes connect timeout
        httpRequest.setReadTimeout(2 * 60000);  // 2 minutes read timeout
      }
    };
  }
}
