package com.netflix.kayenta.graphite.config;

import lombok.Getter;
import lombok.Setter;

public class GraphiteConfigurationTestControllerDefaultProperties {

    @Getter
    @Setter
    private String scope;

    @Getter
    @Setter
    private String start;

    @Getter
    @Setter
    private String end;
}
