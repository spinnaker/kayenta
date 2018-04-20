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
@JsonTypeName("wavefront")
public class WavefrontCanaryMetricSetQueryConfig implements CanaryMetricSetQueryConfig {

    public static final String SERVICE_TYPE = "wavefront";

    @NotNull
    @Getter
    private String metricName;

    @NotNull
    @Getter
    private String aggregate;

    @Override
    public String getServiceType() {
        return SERVICE_TYPE;
    }
}
