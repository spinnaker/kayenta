package com.netflix.kayenta.datadog.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.canary.CanaryMetricConfig;
import com.netflix.kayenta.canary.CanaryScope;
import com.netflix.kayenta.canary.providers.metrics.DatadogCanaryMetricSetQueryConfig;
import com.netflix.kayenta.canary.providers.metrics.DatadogCanaryMetricSetQueryConfig.MetricType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DatadogMetricsServiceTest {

  @Mock private List<String> mockAccountNames;

  @Mock private CanaryConfig mockCanaryConfig;

  @Mock private DatadogCanaryMetricSetQueryConfig mockDatadogCanaryMetricSetQueryConfig;

  @Mock private CanaryMetricConfig mockCanaryMetricConfig;

  @Mock private CanaryScope mockCanaryScope;

  private final String metricsAccountName = "accountName";

  private DatadogMetricsService datadogMetricsService;

  private List<String> tags;

  @BeforeEach
  public void setUp() {
    tags = Arrays.asList(new String[] {"status:200", "endpoint:getListings"});
    datadogMetricsService = new datadogMetricsService(mockAccountNames);
    when(mockCanaryScope.getScope()).thenReturn("kube_namespace:dora-canary");
    when(mockDatadogCanaryMetricSetQueryConfig.getMetricName())
        .thenReturn("services_platform.service.response.p95");
    when(mockCanaryMetricConfig.getQuery()).thenReturn(mockDatadogCanaryMetricSetQueryConfig);
    when(mockDatadogCanaryMetricSetQueryConfig.getTags()).thenReturn(tags);
  }

  private String buildQuery() {
    return datadogMetricsService.buildQuery(
        metricsAccountName, mockCanaryConfig, mockCanaryMetricConfig, mockCanaryScope);
  }

  @Test
  public void buildCountQuery() {
    when(mockDatadogCanaryMetricSetQueryConfig.getMetricType()).thenReturn(MetricType.COUNT);
    String query = buildQuery();
    assertEquals(
        query,
        "sum:services_platform.service.response.p95{kube_namespace:dora-canary,status:200,endpoint:getListings}.as_count()");
  }

  @Test
  public void buildGaugeQuery() {
    when(mockDatadogCanaryMetricSetQueryConfig.getMetricType()).thenReturn(MetricType.GAUGE);
    String query = buildQuery();
    assertEquals(
        query,
        "services_platform.service.response.p95{kube_namespace:dora-canary,status:200,endpoint:getListings}");
  }

  @Test
  public void buildRateQuery() {
    when(mockDatadogCanaryMetricSetQueryConfig.getMetricType()).thenReturn(MetricType.RATE);
    String query = buildQuery();
    assertEquals(
        query,
        "services_platform.service.response.p95{kube_namespace:dora-canary,status:200,endpoint:getListings}");
  }

  @Test
  public void buildQueryWithoutTags() {
    when(mockDatadogCanaryMetricSetQueryConfig.getMetricType()).thenReturn(MetricType.GAUGE);
    when(mockDatadogCanaryMetricSetQueryConfig.getTags()).thenReturn(Collections.emptyList());
    String query = buildQuery();
    assertEquals(query, "services_platform.service.response.p95{kube_namespace:dora-canary}");
  }
}
