package com.netflix.kayenta.config;

import java.time.Instant;
import java.util.Random;

class GraphiteMetricProvider {
    private int min;
    private int max;
    private String metricName;

    GraphiteMetricProvider(int min, int max, String metricName) {
        this.min = min;
        this.max = max;
        this.metricName = metricName;
    }

    String getRandomMetricWithinRange() {
        return String.format("%s %d %d%n", metricName,
                new Random().nextInt((max - min) + 1) + min,
                Instant.now().getEpochSecond());
    }
}

