/*
 * Copyright 2018 Google, Inc.
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

package com.netflix.kayenta.prometheus.config;

import lombok.Getter;
import lombok.Setter;

/**
 * This configuration class allows you to specify default values for the PrometheusFetchController.
 */
public class PrometheusConfigurationTestControllerDefaultProperties {

  @Getter
  @Setter
  private String project;

  @Getter
  @Setter
  private String resourceType;

  @Getter
  @Setter
  private String region;

  @Getter
  @Setter
  private String scope;

  @Getter
  @Setter
  private String start;

  @Getter
  @Setter
  private String end;
}
