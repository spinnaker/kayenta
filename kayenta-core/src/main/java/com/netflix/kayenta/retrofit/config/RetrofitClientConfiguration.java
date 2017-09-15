/*
 * Copyright 2016 Netflix, Inc.
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

package com.netflix.kayenta.retrofit.config;

import com.netflix.spinnaker.config.OkHttpClientConfiguration;
import com.netflix.spinnaker.orca.retrofit.exceptions.RetrofitExceptionHandler;
import com.squareup.okhttp.ConnectionPool;
import com.squareup.okhttp.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration
public class RetrofitClientConfiguration {

  @Value("${okHttpClient.connectionPool.maxIdleConnections:5}")
  int maxIdleConnections;

  @Value("${okHttpClient.connectionPool.keepAliveDurationMs:300000}")
  int keepAliveDurationMs;

  @Value("${okHttpClient.retryOnConnectionFailure:true}")
  boolean retryOnConnectionFailure;

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  OkHttpClient okHttpClient(OkHttpClientConfiguration okHttpClientConfig) {
    OkHttpClient okHttpClient = okHttpClientConfig.create();
    okHttpClient.setConnectionPool(new ConnectionPool(maxIdleConnections, keepAliveDurationMs));
    okHttpClient.setRetryOnConnectionFailure(retryOnConnectionFailure);
    return okHttpClient;
  }

  @Bean @Order(Ordered.HIGHEST_PRECEDENCE)
  RetrofitExceptionHandler retrofitExceptionHandler() {
    return new RetrofitExceptionHandler();
  }
}
