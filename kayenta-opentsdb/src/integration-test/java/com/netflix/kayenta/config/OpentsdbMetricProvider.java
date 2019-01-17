package com.netflix.kayenta.config;

import java.time.Instant;
import java.util.Random;

class OpentsdbMetricProvider {
    private int min;
    private int max;
    private String metricName;
    private String tags;

    OpentsdbMetricProvider(int min, int max, String metricName, String tags) {
        this.min = min;
        this.max = max;
        this.metricName = metricName;
        this.tags = tags;
    }

    String getRandomMetricWithinRange() {
        return String.format("put %s %d %d %s%n", metricName,
              Instant.now().getEpochSecond(),
                new Random().nextInt((max - min) + 1) + min, tags);
    }
}

