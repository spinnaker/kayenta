package com.netflix.kayenta.wavefront.metrics;

import com.netflix.kayenta.canary.CanaryMetricConfig;
import com.netflix.kayenta.canary.CanaryScope;
import com.netflix.kayenta.canary.providers.metrics.WavefrontCanaryMetricSetQueryConfig;
import com.netflix.kayenta.wavefront.canary.WavefrontCanaryScope;
import org.junit.Test;

import java.time.Instant;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class WavefrontMetricsServiceTest {

    private WavefrontMetricsService wavefrontMetricsService = new WavefrontMetricsService(null, null, null);
    private static final String METRIC_NAME = "example.metric.name";
    private static final String SCOPE = "env=test";
    private static final String AGGREGATE= "avg";

    @Test
    public void testBuildQuery_NoScopeProvided() {
        CanaryScope canaryScope = createScope("");
        CanaryMetricConfig canaryMetricSetQueryConfig = queryConfig(AGGREGATE);
        String query = wavefrontMetricsService.buildQuery("", null, canaryMetricSetQueryConfig, canaryScope );
        assertThat(query, is(AGGREGATE +"(ts("+METRIC_NAME+"))"));
    }

    @Test
    public void testBuildQuery_NoAggregateProvided() {
        CanaryScope canaryScope = createScope(SCOPE);
        CanaryMetricConfig canaryMetricSetQueryConfig = queryConfig("");
        String query = wavefrontMetricsService.buildQuery("", null, canaryMetricSetQueryConfig, canaryScope );
        assertThat(query, is("ts("+METRIC_NAME+", " + SCOPE +")"));
    }

    @Test
    public void testBuildQuery_ScopeAndAggregateProvided() {
        CanaryScope canaryScope = createScope(SCOPE);
        CanaryMetricConfig canaryMetricSetQueryConfig = queryConfig("avg");
        String query = wavefrontMetricsService.buildQuery("", null, canaryMetricSetQueryConfig, canaryScope );
        assertThat(query, is(AGGREGATE +"(ts("+METRIC_NAME+", "+SCOPE +"))"));
    }

    private CanaryMetricConfig queryConfig(String aggregate) {
        WavefrontCanaryMetricSetQueryConfig wavefrontCanaryMetricSetQueryConfig = WavefrontCanaryMetricSetQueryConfig
                .builder()
                .aggregate(aggregate)
                .metricName(METRIC_NAME)
                .build();
        CanaryMetricConfig queryConfig = CanaryMetricConfig.builder().query(wavefrontCanaryMetricSetQueryConfig).build();
        return queryConfig;
    }

    private CanaryScope createScope(String scope) {
        WavefrontCanaryScope canaryScope = new WavefrontCanaryScope();
        canaryScope.setScope(scope);
        return canaryScope;
    }
}
