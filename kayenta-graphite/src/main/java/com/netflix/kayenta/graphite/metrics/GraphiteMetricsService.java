package com.netflix.kayenta.graphite.metrics;

import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.canary.CanaryMetricConfig;
import com.netflix.kayenta.canary.CanaryScope;
import com.netflix.kayenta.canary.providers.metrics.GraphiteCanaryMetricSetQueryConfig;
import com.netflix.kayenta.canary.providers.metrics.QueryConfigUtils;
import com.netflix.kayenta.graphite.model.GraphiteResults;
import com.netflix.kayenta.graphite.security.GraphiteNamedAccountCredentials;
import com.netflix.kayenta.graphite.service.GraphiteRemoteService;
import com.netflix.kayenta.metrics.MetricSet;
import com.netflix.kayenta.metrics.MetricsService;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.spectator.api.Registry;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Builder
@Slf4j
public class GraphiteMetricsService implements MetricsService {
    private static final String DEFAULT_FORMAT = "json";

    @NotNull
    @Singular
    @Getter
    private List<String> accountNames;

    @Autowired
    private final AccountCredentialsRepository accountCredentialsRepository = null;

    @Autowired
    private final Registry registry = null;

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

        String query = String.format("%s.%s", queryConfig.getMetricName(), canaryScope.getScope());
        log.info("Query sent to graphite: {}.", query);

        List<GraphiteResults> graphiteResultsList = remoteService.rangeQuery(
                query,
                (int)canaryScope.getStart().getEpochSecond(),
                (int)canaryScope.getEnd().getEpochSecond(),
                DEFAULT_FORMAT
        );

        List<MetricSet> metricSets = new ArrayList<>();

        for (GraphiteResults entry : graphiteResultsList) {
            metricSets.add(
                    MetricSet.builder()
                            .name(canaryMetricConfig.getName())
                            .startTimeMillis(entry.getStart() * 1000)
                            .startTimeIso(Instant.ofEpochSecond(entry.getStart()).toString())
                            .endTimeMillis(entry.getEnd() * 1000)
                            .endTimeIso(Instant.ofEpochSecond(entry.getEnd()).toString())
                            .stepMillis(entry.getInterval() * 1000)
                            .values(entry.getDataPoints().collect(Collectors.toList()))
                            .build()
            );
        }

        return metricSets;
    }
}
