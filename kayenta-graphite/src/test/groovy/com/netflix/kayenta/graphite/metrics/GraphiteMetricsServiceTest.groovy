package com.netflix.kayenta.graphite.metrics

import com.netflix.kayenta.canary.CanaryConfig
import com.netflix.kayenta.canary.providers.metrics.GraphiteCanaryMetricSetQueryConfig
import com.netflix.kayenta.canary.providers.metrics.QueryConfigUtils
import com.netflix.kayenta.graphite.cancary.GraphiteCanaryScope
import spock.lang.Specification
import spock.lang.Unroll

class GraphiteMetricsServiceTest extends Specification {
    @Unroll
    void "template #customerFilterTemplate expands properly"() {
        given:
        CanaryConfig canaryConfig = CanaryConfig.builder().templates(templates).build()
        GraphiteCanaryMetricSetQueryConfig graphiteCanaryMetricSetQueryConfig =
        GraphiteCanaryMetricSetQueryConfig.builder().customFilterTemplate(customFilterTemplate).build();
        GraphiteCanaryScope graphiteCanaryScope = new GraphiteCanaryScope()
        graphiteCanaryScope.setScope("value-1")

        expect:
        QueryConfigUtils.expandCustomFilter(canaryConfig, graphiteCanaryMetricSetQueryConfig, graphiteCanaryScope, ["scope"] as String[]) == expectedExpandedTemplate

        where:
        templates                                             | customFilterTemplate || expectedExpandedTemplate
        ["my-template": 'A test: ${scope}.200']               | "my-template"        || "A test: value-1.200"
    }
}
