package com.netflix.kayenta.graphite.metrics;

import com.google.common.collect.Lists;
import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.canary.CanaryMetricConfig;
import com.netflix.kayenta.canary.CanaryScope;
import com.netflix.kayenta.canary.providers.metrics.GraphiteCanaryMetricSetQueryConfig;
import com.netflix.kayenta.graphite.model.GraphiteMetricDescriptor;
import com.netflix.kayenta.graphite.model.GraphiteMetricDescriptorsResponse;
import com.netflix.kayenta.graphite.model.GraphiteResults;
import com.netflix.kayenta.graphite.security.GraphiteNamedAccountCredentials;
import com.netflix.kayenta.graphite.service.GraphiteRemoteService;
import com.netflix.kayenta.metrics.MetricSet;
import com.netflix.kayenta.metrics.MetricsService;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.spectator.api.Registry;

import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;


@Builder
@Slf4j
public class GraphiteMetricsService implements MetricsService {
    private static final String DEFAULT_FORMAT = "json";
    private static final String DEFAULT_DESCRIPTOR_FORMAT = "completer";
    private static final String SCOPE_VARIABLE = "$scope";
    private static final String LOCATION_VARIABLE = "$location";
    private static final String DELIMITER = ".";
    private static final String GRAPHITE_QUERY_WILDCARD = "*";
    private static final String GRAPHITE_IS_LEAF = "1";

    @NotNull
    @Singular
    @Getter
    private List<String> accountNames;

    @Autowired
    private final AccountCredentialsRepository accountCredentialsRepository = null;

    @Autowired
    private final Registry registry = null;

    @Builder.Default
    private List<GraphiteMetricDescriptor> metricDescriptorsCache = Collections.emptyList();

    @Override
    public String getType() {
        return GraphiteCanaryMetricSetQueryConfig.SERVICE_TYPE;
    }

    @Override
    public boolean servicesAccount(String accountName) {
        return accountNames.contains(accountName);
    }

    @Override
    public List<MetricSet> queryMetrics(String metricsAccountName,
                                        CanaryConfig canaryConfig,
                                        CanaryMetricConfig canaryMetricConfig,
                                        CanaryScope canaryScope) throws IOException {
        GraphiteNamedAccountCredentials accountCredentials =
                (GraphiteNamedAccountCredentials) accountCredentialsRepository.getOne(metricsAccountName)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Unable to resolve account %s.", metricsAccountName)));

        GraphiteRemoteService remoteService = accountCredentials.getGraphiteRemoteService();
        GraphiteCanaryMetricSetQueryConfig queryConfig =
                (GraphiteCanaryMetricSetQueryConfig) canaryMetricConfig.getQuery();

        String query = String.format("template(%s, scope=\"%s\", location=\"%s\")",
                queryConfig.getMetricName(), canaryScope.getScope(), canaryScope.getLocation());
        log.debug("Query sent to graphite: {}.", query);

        List<GraphiteResults> graphiteResultsList = remoteService.rangeQuery(
                query,
                canaryScope.getStart().getEpochSecond(),
                canaryScope.getEnd().getEpochSecond(),
                DEFAULT_FORMAT
        );

        List<MetricSet> metricSets = new ArrayList<>();

        for (GraphiteResults entry : graphiteResultsList) {
            metricSets.add(
                    MetricSet.builder()
                            .name(canaryMetricConfig.getName())
                            .startTimeMillis(entry.getStartMills())
                            .startTimeIso(Instant.ofEpochSecond(entry.getStart()).toString())
                            .endTimeMillis(entry.getEndMills())
                            .endTimeIso(Instant.ofEpochSecond(entry.getEnd()).toString())
                            .stepMillis(entry.getIntervalMills())
                            .values(entry.getDataPoints().collect(Collectors.toList()))
                            .build()
            );
        }

        return metricSets;
    }

    @Override
    public List<Map> getMetadata(String metricsAccountName, String filter) throws IOException {
        log.debug(String.format("Getting metadata for %s with filter %s", metricsAccountName, filter));

        String baseFilter = "";
        if (filter.contains(DELIMITER)) {
            baseFilter = filter.substring(0, filter.lastIndexOf(DELIMITER) + 1);
        }

        List<Map> result = new LinkedList<>();

        boolean needSpecialDescriptors = filter.contains(DELIMITER)
                && filter.substring(filter.lastIndexOf(DELIMITER)).contains("$");

        if (needSpecialDescriptors) {
            result.addAll(getSpecialMetricDescriptors(baseFilter));
        } else {
            GraphiteNamedAccountCredentials accountCredentials =
                    (GraphiteNamedAccountCredentials) accountCredentialsRepository.getOne(metricsAccountName)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    String.format("Unable to resolve account %s.", metricsAccountName)));

            GraphiteRemoteService remoteService = accountCredentials.getGraphiteRemoteService();

            filter = convertFilterToGraphiteQuery(filter);

            GraphiteMetricDescriptorsResponse graphiteMetricDescriptorsResponse =
                    remoteService.findMetrics(filter, DEFAULT_DESCRIPTOR_FORMAT);

            log.debug(String.format("Getting response for %s with response size %d",
                    metricsAccountName, graphiteMetricDescriptorsResponse.getMetrics().size()));

            String finalBaseFilter = baseFilter;
            Set<String> resultSet = graphiteMetricDescriptorsResponse
                    .getMetrics()
                    .stream()
                    .map(metricDescriptorResponseEntity -> {
                        if (GRAPHITE_IS_LEAF.equals(metricDescriptorResponseEntity.getIsLeaf())) {
                            return finalBaseFilter + metricDescriptorResponseEntity.getName();
                        } else {
                            return finalBaseFilter + metricDescriptorResponseEntity.getName() + DELIMITER;
                        }
                    }).collect(Collectors.toSet());

            resultSet.stream().forEach( name -> result.add(new GraphiteMetricDescriptor(name).getMap()));
        }

        return result;
    }

    private List<Map> getSpecialMetricDescriptors(String baseFilter) {
        return Arrays.asList(
                new GraphiteMetricDescriptor(baseFilter + SCOPE_VARIABLE).getMap(),
                new GraphiteMetricDescriptor(baseFilter + LOCATION_VARIABLE).getMap());
    }

    private String convertFilterToGraphiteQuery(String filter) {
        return filter
                .replace(SCOPE_VARIABLE, GRAPHITE_QUERY_WILDCARD)
                .replace(LOCATION_VARIABLE, GRAPHITE_QUERY_WILDCARD);
    }
}
