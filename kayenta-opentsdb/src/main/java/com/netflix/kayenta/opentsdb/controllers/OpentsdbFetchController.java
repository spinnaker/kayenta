package com.netflix.kayenta.opentsdb.controllers;

import com.netflix.kayenta.opentsdb.canary.OpentsdbCanaryScope;
import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.canary.CanaryMetricConfig;
import com.netflix.kayenta.canary.providers.metrics.OpentsdbCanaryMetricSetQueryConfig;
import com.netflix.kayenta.metrics.SynchronousQueryProcessor;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.kayenta.security.CredentialsHelper;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/fetch/opentsdb")
@Slf4j
public class OpentsdbFetchController {

  private final AccountCredentialsRepository accountCredentialsRepository;
  private final SynchronousQueryProcessor synchronousQueryProcessor;

  @Autowired
  public OpentsdbFetchController(AccountCredentialsRepository accountCredentialsRepository, SynchronousQueryProcessor synchronousQueryProcessor) {
    this.accountCredentialsRepository = accountCredentialsRepository;
    this.synchronousQueryProcessor = synchronousQueryProcessor;
  }

  @RequestMapping(value = "/query", method = RequestMethod.POST)
  public Map queryMetrics(@RequestParam(required = false) final String metricsAccountName,
                          @RequestParam(required = false) final String storageAccountName,
                          @ApiParam(defaultValue = "sum:rate:tc.proc.stat.cpu.total.coreapp-ngapp-prod") @RequestParam String m,
                          @ApiParam(defaultValue = "cpu") @RequestParam String metricSetName,
                          @ApiParam(defaultValue = "stage=prod") @RequestParam String scope,
                          @ApiParam(defaultValue = "2000-01-01T00:00:00Z") @RequestParam Instant start,
                          @ApiParam(defaultValue = "2000-01-01T04:00:00Z") @RequestParam Instant end) throws IOException {
    String resolvedMetricsAccountName = CredentialsHelper.resolveAccountByNameOrType(metricsAccountName,
            AccountCredentials.Type.METRICS_STORE,
            accountCredentialsRepository);
    String resolvedStorageAccountName = CredentialsHelper.resolveAccountByNameOrType(storageAccountName,
            AccountCredentials.Type.OBJECT_STORE,
            accountCredentialsRepository);

    OpentsdbCanaryMetricSetQueryConfig opentsdbCanaryMetricSetQueryConfig =
            OpentsdbCanaryMetricSetQueryConfig
                    .builder()
                    .metricName(m)
                    .build();
    CanaryMetricConfig canaryMetricConfig =
            CanaryMetricConfig
                    .builder()
                    .name(metricSetName)
                    .query(opentsdbCanaryMetricSetQueryConfig)
                    .build();

    OpentsdbCanaryScope opentsdbCanaryScope = new OpentsdbCanaryScope();
    opentsdbCanaryScope.setScope(scope);
    opentsdbCanaryScope.setStart(start);
    opentsdbCanaryScope.setEnd(end);

    String metricSetListId = synchronousQueryProcessor.executeQuery(resolvedMetricsAccountName,
            resolvedStorageAccountName,
            CanaryConfig.builder().metric(canaryMetricConfig).build(),
            0,
            opentsdbCanaryScope);

    return Collections.singletonMap("metricSetListId", metricSetListId);
  }
}
