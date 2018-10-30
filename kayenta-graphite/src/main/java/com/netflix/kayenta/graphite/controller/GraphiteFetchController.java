package com.netflix.kayenta.graphite.controller;

import com.google.common.base.Strings;
import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.canary.CanaryMetricConfig;
import com.netflix.kayenta.canary.CanaryScope;
import com.netflix.kayenta.canary.providers.metrics.GraphiteCanaryMetricSetQueryConfig;
import com.netflix.kayenta.graphite.config.GraphiteConfigurationTestControllerDefaultProperties;
import com.netflix.kayenta.metrics.SynchronousQueryProcessor;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.kayenta.security.CredentialsHelper;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static com.netflix.kayenta.canary.util.FetchControllerUtils.determineDefaultProperty;

@RestController
@RequestMapping("/fetch/graphite")
@Slf4j
public class GraphiteFetchController {
    private final AccountCredentialsRepository accountCredentialsRepository;
    private final SynchronousQueryProcessor synchronousQueryProcessor;
    private final GraphiteConfigurationTestControllerDefaultProperties graphiteConfigurationTestControllerDefaultProperties;

    @Autowired
    public GraphiteFetchController(AccountCredentialsRepository accountCredentialsRepository,
                                   SynchronousQueryProcessor synchronousQueryProcessor,
                                   GraphiteConfigurationTestControllerDefaultProperties graphiteConfigurationTestControllerDefaultProperties) {
        this.accountCredentialsRepository = accountCredentialsRepository;
        this.synchronousQueryProcessor = synchronousQueryProcessor;
        this.graphiteConfigurationTestControllerDefaultProperties =
                graphiteConfigurationTestControllerDefaultProperties;
    }

    @RequestMapping(value = "/query", method = RequestMethod.POST)
    public Map queryMetrics(@RequestParam(required = false) final String metricsAccountName,
                            @RequestParam(required = false) final String storageAccountName,
                            @ApiParam(defaultValue = "cpu") @RequestParam String metricSetName,
                            @ApiParam(defaultValue = "system.cpu.user") @RequestParam String metricName,
                            @ApiParam(value = "The name of the resource to use when scoping the query. " +
                                    "The most common use-case is to provide a server group name.")
                                @RequestParam(required = false) String scope,
                            @ApiParam(value = "An ISO format timestamp, e.g.: 2018-03-15T01:23:45Z")
                            @RequestParam String start,
                            @ApiParam(value = "An ISO format timestamp, e.g.: 2018-03-15T01:23:45Z")
                            @RequestParam String end) throws IOException {

        start = determineDefaultProperty(start, "start", graphiteConfigurationTestControllerDefaultProperties);
        end = determineDefaultProperty(end, "end", graphiteConfigurationTestControllerDefaultProperties);
        scope = determineDefaultProperty(scope, "scope", graphiteConfigurationTestControllerDefaultProperties);

        if (StringUtils.isEmpty(start)) {
            throw new IllegalArgumentException("Start time is required.");
        }

        if (StringUtils.isEmpty(end)) {
            throw new IllegalArgumentException("End time is required.");
        }

        String resolvedMetricsAccountName = CredentialsHelper.resolveAccountByNameOrType(metricsAccountName,
                AccountCredentials.Type.METRICS_STORE,
                accountCredentialsRepository);
        String resolvedStorageAccountName = CredentialsHelper.resolveAccountByNameOrType(storageAccountName,
                AccountCredentials.Type.OBJECT_STORE,
                accountCredentialsRepository);

        GraphiteCanaryMetricSetQueryConfig.GraphiteCanaryMetricSetQueryConfigBuilder
                graphiteCanaryMetricSetQueryConfigBuilder = GraphiteCanaryMetricSetQueryConfig.builder();

        graphiteCanaryMetricSetQueryConfigBuilder.metricName(metricName);

        CanaryMetricConfig canaryMetricConfig =
                CanaryMetricConfig.builder()
                        .name(metricSetName)
                        .query(graphiteCanaryMetricSetQueryConfigBuilder.build())
                        .build();

        CanaryScope canaryScope = new CanaryScope(scope, null, Instant.parse(start),
                Instant.parse(end), null, Collections.EMPTY_MAP);

        String metricsSetListId =
                synchronousQueryProcessor.processQuery(resolvedMetricsAccountName,
                        resolvedStorageAccountName,
                        CanaryConfig.builder().metric(canaryMetricConfig).build(),
                        0,
                        canaryScope);
        return Collections.singletonMap("metricSetListId", metricsSetListId);
    }
}
