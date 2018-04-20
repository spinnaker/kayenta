package com.netflix.kayenta.wavefront.controller;

import com.netflix.kayenta.canary.CanaryMetricConfig;
import com.netflix.kayenta.canary.providers.metrics.WavefrontCanaryMetricSetQueryConfig;
import com.netflix.kayenta.metrics.SynchronousQueryProcessor;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.kayenta.security.CredentialsHelper;
import com.netflix.kayenta.wavefront.canary.WavefrontCanaryScope;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/fetch/wavefront")
@Slf4j
public class WavefrontFetchController {

    private final AccountCredentialsRepository accountCredentialsRepository;
    private final SynchronousQueryProcessor synchronousQueryProcessor;

    @Autowired
    public WavefrontFetchController(AccountCredentialsRepository accountCredentialsRepository, SynchronousQueryProcessor synchronousQueryProcessor) {
        this.accountCredentialsRepository = accountCredentialsRepository;
        this.synchronousQueryProcessor = synchronousQueryProcessor;
    }

    @RequestMapping(value = "/query", method = RequestMethod.POST)
    public Map queryMetrics(@RequestParam(required = false) final String metricsAccountName,
                            @RequestParam(required = false) final String storageAccountName,
                            @ApiParam(defaultValue = "cpu") @RequestParam String metricSetName,
                            @ApiParam(defaultValue = "system.cpu.user") @RequestParam String metricName,
                            @ApiParam(value = "An aggregate function, e.g.: avg, min, max")
                            @RequestParam(defaultValue = "") String aggregate,
                            @ApiParam(value = "The scope of the Wavefront query. e.g. autoscaling_group=myapp-prd-v002")
                            @RequestParam(defaultValue = "") String scope,
                            @ApiParam(value = "An ISO format timestamp, e.g.: 2018-03-15T01:23:45Z")
                            @RequestParam String start,
                            @ApiParam(value = "An ISO format timestamp, e.g.: 2018-03-15T01:23:45Z")
                            @RequestParam String end,
                            @ApiParam(defaultValue = "m", value = "[s, m, h, d]") @RequestParam String step,
                            @RequestParam(required = false) final boolean dryRun) throws IOException {

        String resolvedMetricsAccountName = CredentialsHelper.resolveAccountByNameOrType(metricsAccountName,
                AccountCredentials.Type.METRICS_STORE,
                accountCredentialsRepository);
        String resolvedStorageAccountName = CredentialsHelper.resolveAccountByNameOrType(storageAccountName,
                AccountCredentials.Type.OBJECT_STORE,
                accountCredentialsRepository);

        WavefrontCanaryMetricSetQueryConfig wavefrontCanaryMetricSetQueryConfig = WavefrontCanaryMetricSetQueryConfig
                .builder()
                .metricName(metricName)
                .aggregate(aggregate)
                .build();

        CanaryMetricConfig canaryMetricConfig =
                CanaryMetricConfig
                        .builder()
                        .name(metricSetName)
                        .query(wavefrontCanaryMetricSetQueryConfig)
                        .build();

        WavefrontCanaryScope wavefrontCanaryScope = new WavefrontCanaryScope();
        wavefrontCanaryScope.setScope(scope);
        wavefrontCanaryScope.setStart(Instant.parse(start));
        wavefrontCanaryScope.setEnd(Instant.parse(end));
        wavefrontCanaryScope.setGranularity(step);
        wavefrontCanaryScope.setStepFromGranularity(step);

        return synchronousQueryProcessor.processQueryAndReturnMap(resolvedMetricsAccountName,
                resolvedStorageAccountName,
                null,
                canaryMetricConfig,
                0,
                wavefrontCanaryScope,
                dryRun);
    }
}