package com.netflix.kayenta.canary.providers.metrics;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.netflix.kayenta.canary.CanaryMetricSetQueryConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;


import javax.validation.constraints.NotNull;

@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("graphite")
public class GraphiteCanaryMetricSetQueryConfig implements CanaryMetricSetQueryConfig {

    public static final String SERVICE_TYPE = "graphite";

    @NotNull
    @Getter
    private String metricName;

    @Getter
    private String customFilterTemplate;

    @Override
    public String getServiceType() {
        return SERVICE_TYPE;
    }
}
