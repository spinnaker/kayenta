package com.netflix.kayenta.stackdriver.metrics;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.google.api.services.monitoring.v3.model.ListTimeSeriesResponse;
import com.google.api.services.monitoring.v3.model.Point;
import com.google.api.services.monitoring.v3.model.TimeSeries;
import com.google.api.services.monitoring.v3.model.TypedValue;
import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.canary.CanaryMetricConfig;
import com.netflix.kayenta.metrics.MetricSet;
import com.netflix.kayenta.stackdriver.canary.StackdriverCanaryScope;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StackdriverMetricsServiceTest {
  private static final String ACCOUNT_NAME = "some-stackdriver-account";
  private static final String SCOPE = "env=test";
  private static final double testDoubleValue = 12.3d;

  @Mock ListTimeSeriesResponse response;

  @InjectMocks StackdriverMetricsService stackdriverMetricsService;

  @Test
  public void testDummy() {
    assertThat("dummy", is("dummy"));
  }

  @Test
  public void returnsMetricSetValuesFromDouble() {
    TimeSeries singleDoublePointTimeSeries =
        new TimeSeries()
            .setValueType("DOUBLE")
            .setPoints(
                new ArrayList<Point>() {
                  {
                    add((new Point()).setValue((new TypedValue()).setDoubleValue(testDoubleValue)));
                  }
                });

    when(response.getTimeSeries())
        .thenReturn(
            new ArrayList<TimeSeries>() {
              {
                add(singleDoublePointTimeSeries);
              }
            });

    CanaryConfig canaryConfig = new CanaryConfig();
    CanaryMetricConfig canaryMetricConfig = CanaryMetricConfig.builder().name("some-name").build();
    StackdriverCanaryScope experimentScope = new StackdriverCanaryScope();

    List<MetricSet> metricSetList =
        stackdriverMetricsService.queryMetrics(
            ACCOUNT_NAME, canaryConfig, canaryMetricConfig, experimentScope);

    assertThat(metricSetList.get(0).getValues(), hasItem(testDoubleValue));
  }
}
