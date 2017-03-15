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

package com.netflix.kayenta.stackdriver.metrics;

import com.google.api.services.monitoring.v3.Monitoring;
import com.google.api.services.monitoring.v3.model.ListTimeSeriesResponse;
import com.netflix.kayenta.google.security.GoogleNamedAccountCredentials;
import com.netflix.kayenta.metrics.MetricsService;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Builder
public class StackdriverMetricsService implements MetricsService {

  @NotNull
  @Singular
  @Getter
  private List<String> accountNames;

  @Autowired
  AccountCredentialsRepository accountCredentialsRepository;

  @Override
  public boolean servicesAccount(String accountName) {
    return accountNames.contains(accountName);
  }

  @Override
  public Optional<Map> queryMetrics(String accountName,
                                    String instanceNamePrefix,
                                    String intervalStartTime,
                                    String intervalEndTime) throws IOException {
    GoogleNamedAccountCredentials credentials = (GoogleNamedAccountCredentials)accountCredentialsRepository
      .getOne(accountName)
      .orElseThrow(() -> new IllegalArgumentException("Unable to resolve account " + accountName + "."));
    Monitoring monitoring = credentials.getMonitoring();
    // Some sample query parameters (mainly leaving all of these here so that I remember the api).
    ListTimeSeriesResponse response = monitoring
      .projects()
      .timeSeries()
      .list("projects/" + credentials.getProject())
      .setAggregationAlignmentPeriod("3600s")
      .setAggregationCrossSeriesReducer("REDUCE_MEAN")
      .setAggregationGroupByFields(Arrays.asList("metric.label.instance_name"))
      .setAggregationPerSeriesAligner("ALIGN_MEAN")
      .setFilter("metric.type=\"compute.googleapis.com/instance/cpu/utilization\" AND metric.label.instance_name=starts_with(\"" + instanceNamePrefix + "\")")
      .setIntervalStartTime(intervalStartTime)
      .setIntervalEndTime(intervalEndTime)
      .execute();

    return Optional.of(response);
  }
}
