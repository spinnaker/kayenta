package com.netflix.kayenta.datadog.orca;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.canary.CanaryScope;
import com.netflix.kayenta.metrics.SynchronousQueryProcessor;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.kayenta.security.CredentialsHelper;
import com.netflix.spinnaker.orca.RetryableTask;
import com.netflix.spinnaker.orca.TaskResult;
import com.netflix.spinnaker.orca.pipeline.model.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Component
@Slf4j
public class DatadogFetchTask implements RetryableTask {
  private final ObjectMapper kayentaObjectMapper;
  private final AccountCredentialsRepository accountCredentialsRepository;
  private final SynchronousQueryProcessor synchronousQueryProcessor;

  @Autowired
  public DatadogFetchTask(ObjectMapper kayentaObjectMapper,
                          AccountCredentialsRepository accountCredentialsRepository,
                          SynchronousQueryProcessor synchronousQueryProcessor) {
    this.kayentaObjectMapper = kayentaObjectMapper;
    this.accountCredentialsRepository = accountCredentialsRepository;
    this.synchronousQueryProcessor = synchronousQueryProcessor;
  }

  @Override
  public long getBackoffPeriod() {
    return Duration.ofSeconds(2).toMillis();
  }

  @Override
  public long getTimeout() {
    return Duration.ofMinutes(2).toMillis();
  }

  @Nonnull
  @Override
  public TaskResult execute(@Nonnull Stage stage) {
    Map<String, Object> context = stage.getContext();
    CanaryScope canaryScope;

    try {
      canaryScope = kayentaObjectMapper.readValue((String)stage.getContext().get("canaryScope"), CanaryScope.class);
    } catch (IOException e) {
      log.warn("Unable to parse JSON scope", e);
      throw new RuntimeException(e);
    }

    String resolvedMetricsAccountName = CredentialsHelper.resolveAccountByNameOrType(
      (String)context.get("metricsAccountName"),
      AccountCredentials.Type.METRICS_STORE,
      accountCredentialsRepository
    );

    String resolvedStorageAccountName = CredentialsHelper.resolveAccountByNameOrType(
      (String)context.get("storageAccountName"),
      AccountCredentials.Type.OBJECT_STORE,
      accountCredentialsRepository
    );

    return synchronousQueryProcessor.processQueryAndProduceTaskResult(
      resolvedMetricsAccountName,
      resolvedStorageAccountName,
      kayentaObjectMapper.convertValue(context.get("canaryConfig"), CanaryConfig.class),
      (Integer)stage.getContext().get("metricIndex"),
      canaryScope
    );
  }
}
