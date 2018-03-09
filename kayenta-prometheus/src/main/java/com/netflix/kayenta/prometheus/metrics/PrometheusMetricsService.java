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

package com.netflix.kayenta.prometheus.metrics;

import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.canary.CanaryMetricConfig;
import com.netflix.kayenta.canary.CanaryScope;
import com.netflix.kayenta.canary.providers.PrometheusCanaryMetricSetQueryConfig;
import com.netflix.kayenta.metrics.MetricSet;
import com.netflix.kayenta.metrics.MetricsService;
import com.netflix.kayenta.prometheus.canary.PrometheusCanaryScope;
import com.netflix.kayenta.prometheus.model.PrometheusMetricDescriptor;
import com.netflix.kayenta.prometheus.model.PrometheusMetricDescriptorsResponse;
import com.netflix.kayenta.prometheus.model.PrometheusResults;
import com.netflix.kayenta.prometheus.security.PrometheusNamedAccountCredentials;
import com.netflix.kayenta.prometheus.service.PrometheusRemoteService;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.kayenta.security.CredentialsHelper;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Builder
@Slf4j
public class PrometheusMetricsService implements MetricsService {

  @NotNull
  private String scopeLabel;

  @NotNull
  @Singular
  @Getter
  private List<String> accountNames;

  @Autowired
  private final AccountCredentialsRepository accountCredentialsRepository;

  @Autowired
  private final Registry registry;

  @Builder.Default
  private List<PrometheusMetricDescriptor> metricDescriptorsCache = Collections.emptyList();

  @Override
  public String getType() {
    return "prometheus";
  }

  @Override
  public boolean servicesAccount(String accountName) {
    return accountNames.contains(accountName);
  }

  private StringBuilder addScopeFilter(StringBuilder queryBuilder,
                                       PrometheusCanaryScope prometheusCanaryScope,
                                       String resourceType,
                                       PrometheusCanaryMetricSetQueryConfig queryConfig) {
    String scope = prometheusCanaryScope.getScope();
    String projectId = prometheusCanaryScope.getProject();
    String region = prometheusCanaryScope.getRegion();
    List<String> filters = queryConfig.getLabelBindings();
    if (filters == null) {
      filters = new ArrayList<>();
    }

    // TODO(duftler): Add support for custom filter templates.
    if ("gce_instance".equals(resourceType)) {
      addGCEFilters(scopeLabel, scope, projectId, region, filters);
    } else if ("aws_ec2_instance".equals(resourceType)) {
      addEC2Filters("asg_groupName", scope, region, filters);
    } else {
      log.warn("There is no explicit support for resourceType '" + resourceType + "'. Your mileage may vary.");
    }
    // TODO(duftler): Add support for K8S resource types.

    if (!filters.isEmpty()) {
      String sep = "";
      queryBuilder.append('{');
      for (String binding : filters) {
        queryBuilder.append(sep);
        queryBuilder.append(binding);
        sep = ",";
      }
      queryBuilder.append('}');
    }

    return queryBuilder;
  }

  private static void addGCEFilters(String scopeLabel, String scope, String projectId, String region, List<String> filters) {
    if (StringUtils.isEmpty(region)) {
      throw new IllegalArgumentException("Region is required when resourceType is 'gce_instance'.");
    }

    if (!StringUtils.isEmpty(scope)) {
      scope += "-.{4}";

      filters.add(scopeLabel + "=~\"" + scope + "\"");
    }

    String zoneRegex = ".+/";

    if (!StringUtils.isEmpty(projectId)) {
      zoneRegex += "projects/" + projectId + "/";
    }

    zoneRegex += "zones/" + region + "-.{1}";
    filters.add("zone=~\"" + zoneRegex + "\"");
  }

  private static void addEC2Filters(String scopeLabel, String scope, String region, List<String> filters) {
    if (StringUtils.isEmpty(region)) {
      throw new IllegalArgumentException("Region is required when resourceType is 'aws_ec2_instance'.");
    }

    if (!StringUtils.isEmpty(scope)) {
      filters.add(scopeLabel + "=\"" + scope + "\"");
    }

    filters.add("zone=~\"" + region + ".{1}\"");
  }

  private static StringBuilder addAvgQuery(StringBuilder queryBuilder) {
    return queryBuilder.insert(0, "avg(").append(")");
  }

  private static StringBuilder addGroupByQuery(StringBuilder queryBuilder, PrometheusCanaryMetricSetQueryConfig queryConfig) {
    List<String> groupByFields = queryConfig.getGroupByFields();

    if (!CollectionUtils.isEmpty(groupByFields)) {
      queryBuilder.append(" by (");
      String sep = "";
      for (String elem : groupByFields) {
        queryBuilder.append(sep);
        queryBuilder.append(elem);
        sep = ",";
      }
      queryBuilder.append(")");
    }
    return queryBuilder;
  }

  @Override
  public List<MetricSet> queryMetrics(String accountName,
                                      CanaryConfig canaryConfig,
                                      CanaryMetricConfig canaryMetricConfig,
                                      CanaryScope canaryScope) throws IOException {
    if (!(canaryScope instanceof PrometheusCanaryScope)) {
      throw new IllegalArgumentException("Canary scope not instance of PrometheusCanaryScope: " + canaryScope);
    }

    PrometheusCanaryScope prometheusCanaryScope = (PrometheusCanaryScope)canaryScope;
    PrometheusNamedAccountCredentials credentials = (PrometheusNamedAccountCredentials)accountCredentialsRepository
      .getOne(accountName)
      .orElseThrow(() -> new IllegalArgumentException("Unable to resolve account " + accountName + "."));
    PrometheusRemoteService prometheusRemoteService = credentials.getPrometheusRemoteService();
    PrometheusCanaryMetricSetQueryConfig queryConfig = (PrometheusCanaryMetricSetQueryConfig)canaryMetricConfig.getQuery();
    String resourceType = prometheusCanaryScope.getResourceType();

    StringBuilder queryBuilder = new StringBuilder(queryConfig.getMetricName());
    queryBuilder = addScopeFilter(queryBuilder, prometheusCanaryScope, resourceType, queryConfig);
    queryBuilder = addAvgQuery(queryBuilder);
    queryBuilder = addGroupByQuery(queryBuilder, queryConfig);

    long startTime = registry.clock().monotonicTime();
    List<PrometheusResults> prometheusResultsList;

    try {
      if (StringUtils.isEmpty(prometheusCanaryScope.getStart())) {
        throw new IllegalArgumentException("Start time is required.");
      }

      if (StringUtils.isEmpty(prometheusCanaryScope.getEnd())) {
        throw new IllegalArgumentException("End time is required.");
      }

      prometheusResultsList = prometheusRemoteService.rangeQuery(queryBuilder.toString(),
                                                                 prometheusCanaryScope.getStart().toString(),
                                                                 prometheusCanaryScope.getEnd().toString(),
                                                                 prometheusCanaryScope.getStep());
    } finally {
      long endTime = registry.clock().monotonicTime();
      // TODO(ewiseblatt/duftler): Add appropriate tags.
      Id prometheusFetchTimerId = registry.createId("prometheus.fetchTime");

      registry.timer(prometheusFetchTimerId).record(endTime - startTime, TimeUnit.NANOSECONDS);
    }

    List<MetricSet> metricSetList = new ArrayList<>();

    if (!CollectionUtils.isEmpty(prometheusResultsList)) {
      for (PrometheusResults prometheusResults : prometheusResultsList) {
        Instant responseStartTimeInstant = Instant.ofEpochMilli(prometheusResults.getStartTimeMillis());
        MetricSet.MetricSetBuilder metricSetBuilder =
          MetricSet.builder()
            .name(canaryMetricConfig.getName())
            .startTimeMillis(prometheusResults.getStartTimeMillis())
            .startTimeIso(responseStartTimeInstant.toString())
            .stepMillis(TimeUnit.SECONDS.toMillis(prometheusResults.getStepSecs()))
            .values(prometheusResults.getValues());

        Map<String, String> tags = prometheusResults.getTags();

        if (tags != null) {
          metricSetBuilder.tags(tags);
        }

        metricSetBuilder.attribute("query", queryBuilder.toString());

        metricSetList.add(metricSetBuilder.build());
      }
    } else {
      MetricSet.MetricSetBuilder metricSetBuilder =
        MetricSet.builder()
          .name(canaryMetricConfig.getName())
          .startTimeMillis(prometheusCanaryScope.getStart().toEpochMilli())
          .startTimeIso(prometheusCanaryScope.getStart().toString())
          .stepMillis(TimeUnit.SECONDS.toMillis(prometheusCanaryScope.getStep()))
          .values(Collections.emptyList());

      metricSetBuilder.attribute("query", queryBuilder.toString());

      metricSetList.add(metricSetBuilder.build());
    }

    return metricSetList;
  }

  @Override
  public List<Map> getMetadata(String metricsAccountName, String filter) throws IOException {
    if (!StringUtils.isEmpty(filter)) {
      String lowerCaseFilter = filter.toLowerCase();

      return metricDescriptorsCache
        .stream()
        .filter(metricDescriptor -> metricDescriptor.getName().toLowerCase().contains(lowerCaseFilter))
        .map(metricDescriptor -> metricDescriptor.getMap())
        .collect(Collectors.toList());
    } else {
      return metricDescriptorsCache
        .stream()
        .map(metricDescriptor -> metricDescriptor.getMap())
        .collect(Collectors.toList());
    }
  }

  @Scheduled(fixedDelayString = "#{@prometheusConfigurationProperties.metadataCachingIntervalMS}")
  public void updateMetricDescriptorsCache() throws IOException {
    Set<AccountCredentials> accountCredentialsSet =
      CredentialsHelper.getAllAccountsOfType(AccountCredentials.Type.METRICS_STORE, accountCredentialsRepository);

    for (AccountCredentials credentials : accountCredentialsSet) {
      if (credentials instanceof PrometheusNamedAccountCredentials) {
        PrometheusNamedAccountCredentials prometheusCredentials = (PrometheusNamedAccountCredentials)credentials;
        PrometheusRemoteService prometheusRemoteService = prometheusCredentials.getPrometheusRemoteService();
        PrometheusMetricDescriptorsResponse prometheusMetricDescriptorsResponse = prometheusRemoteService.listMetricDescriptors();

        if (prometheusMetricDescriptorsResponse != null && prometheusMetricDescriptorsResponse.getStatus().equals("success")) {
          List<String> data = prometheusMetricDescriptorsResponse.getData();

          if (!CollectionUtils.isEmpty(data)) {
            // TODO(duftler): Should we instead be building the union across all accounts? This doesn't seem quite right yet.
            metricDescriptorsCache =
              data
                .stream()
                .map(metricName -> new PrometheusMetricDescriptor(metricName))
                .collect(Collectors.toList());

            log.debug("Updated cache with {} metric descriptors via account {}.", metricDescriptorsCache.size(), prometheusCredentials.getName());
          } else {
            log.debug("While updating cache, found no metric descriptors via account {}.", prometheusCredentials.getName());
          }
        } else {
          log.debug("While updating cache, found no metric descriptors via account {}.", prometheusCredentials.getName());
        }
      }
    }
  }
}
