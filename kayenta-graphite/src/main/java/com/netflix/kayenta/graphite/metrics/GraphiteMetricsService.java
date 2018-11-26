package com.netflix.kayenta.graphite.metrics;

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
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

        String query = String.format("template(%s, scope=\"%s\")",
                queryConfig.getMetricName(), canaryScope.getScope());
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
        GraphiteNamedAccountCredentials accountCredentials =
                (GraphiteNamedAccountCredentials) accountCredentialsRepository.getOne(metricsAccountName)
                        .orElseThrow(() -> new IllegalArgumentException(
                                String.format("Unable to resolve account %s.", metricsAccountName)));

        GraphiteRemoteService remoteService = accountCredentials.getGraphiteRemoteService();

        GraphiteMetricDescriptorsResponse graphiteMetricDescriptorsResponse =
                remoteService.findMetrics(filter, DEFAULT_DESCRIPTOR_FORMAT);
        List<Map> metricDescriptor = graphiteMetricDescriptorsResponse
                .getMetrics()
                .stream()
                .map(metricDescriptorResponseEntity ->
                        new GraphiteMetricDescriptor(metricDescriptorResponseEntity.getName()).getMap())
                .collect(Collectors.toList());
        return metricDescriptor;
    }
}
