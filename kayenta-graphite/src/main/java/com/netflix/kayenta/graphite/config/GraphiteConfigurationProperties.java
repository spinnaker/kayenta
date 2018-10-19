package com.netflix.kayenta.graphite.config;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class GraphiteConfigurationProperties {
    @Getter
    private List<GraphiteManagedAccount> accounts = new ArrayList<>();
}
