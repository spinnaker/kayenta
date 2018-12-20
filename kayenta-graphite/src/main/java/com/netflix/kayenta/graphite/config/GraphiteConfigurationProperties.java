package com.netflix.kayenta.graphite.config;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class GraphiteConfigurationProperties {
    @Getter
    private List<GraphiteManagedAccount> accounts = new ArrayList<>();
}
