package com.netflix.kayenta.stackdriver.metrics;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.netflix.kayenta.canary.CanaryMetricConfig;
import com.netflix.kayenta.canary.CanaryScope;
import com.netflix.kayenta.canary.providers.metrics.StackdriverCanaryMetricSetQueryConfig;
import com.netflix.kayenta.stackdriver.canary.StackdriverCanaryScope;
import com.google.api.services.monitoring.v3.model.TimeSeries;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StackdriverMetricsServiceTest {
  private static final String METRIC_NAME = "example.metric.name";
  private static final String SCOPE = "env=test";
  private static final String AGGREGATE = "avg";

  @Mock ListTimeSeriesResponse response;

  @InjectMocs StackdriverMetricsService stackdriverMetricsService;

  @Test
  public void testDummy() {
    assertThat("dummy", is("dummy"));
  }

  @Test
  public void returnsMetricSetValuesFromDouble() {
    when(response.getTimeSeries())
      .thenReturn(
        // TODO construct List<TimeSeries> that returns points with double values
      );

    List<MetricSet> metricSetList = stackdriverMetricsService.queryMetrics(/*TODO which args?*/);

    assertThat(metricSetList, is(/*TODO expected metricSetList*/)
  }
}

