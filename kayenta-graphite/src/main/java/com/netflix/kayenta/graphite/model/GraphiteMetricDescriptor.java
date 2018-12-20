package com.netflix.kayenta.graphite.model;

import java.util.Collections;
import java.util.Map;

public class GraphiteMetricDescriptor {

    private final String name;
    private final Map<String, String> map;

    public GraphiteMetricDescriptor(String name) {
        this.name = name;
        this.map = Collections.singletonMap("name", name);
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getMap() {
        return map;
    }
}
