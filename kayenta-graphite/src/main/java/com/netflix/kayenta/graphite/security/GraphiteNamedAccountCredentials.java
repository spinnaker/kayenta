package com.netflix.kayenta.graphite.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.netflix.kayenta.canary.providers.metrics.GraphiteCanaryMetricSetQueryConfig;
import com.netflix.kayenta.graphite.service.GraphiteRemoteService;
import com.netflix.kayenta.retrofit.config.RemoteService;
import com.netflix.kayenta.security.AccountCredentials;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import javax.validation.constraints.NotNull;
import java.util.List;

@Builder
@Data
public class GraphiteNamedAccountCredentials implements AccountCredentials<GraphiteCredentials> {
    @NotNull
    private String name;

    @NotNull
    @Singular
    private List<Type> supportedTypes;

    @NotNull
    private GraphiteCredentials credentials;

    @NotNull
    private RemoteService endpoint;

    @Override
    public String getType() {
        return GraphiteCanaryMetricSetQueryConfig.SERVICE_TYPE;
    }

    @JsonIgnore
    GraphiteRemoteService graphiteRemoteService;
}
