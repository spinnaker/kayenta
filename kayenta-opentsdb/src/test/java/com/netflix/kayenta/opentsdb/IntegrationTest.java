/*
 * Copyright 2018 Snap Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.kayenta.opentsdb;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.canary.CanaryMetricConfig;
import com.netflix.kayenta.canary.providers.metrics.OpentsdbCanaryMetricSetQueryConfig;
import com.netflix.kayenta.opentsdb.canary.OpentsdbCanaryScope;
import com.netflix.kayenta.opentsdb.model.OpentsdbResults;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Configuration
@ComponentScan({
    "com.netflix.kayenta.retrofit.config"
})
class TestConfig {
}

@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
class CanaryMetricConfigWithResults {
    @NotNull
    @Getter
    private CanaryMetricConfig canaryMetricConfig;

    @NotNull
    @Getter
    private List<OpentsdbResults> opentsdbResults;
}

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfig.class})
public class IntegrationTest {

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    ObjectMapper objectMapper;

    private void configureObjectMapper(ObjectMapper objectMapper) {
        objectMapper.registerSubtypes(OpentsdbCanaryMetricSetQueryConfig.class);
    }

    private String getFileContent(String filename) throws IOException {
        try (InputStream inputStream = resourceLoader.getResource("classpath:" + filename).getInputStream()) {
            return IOUtils.toString(inputStream, Charsets.UTF_8.name());
        }
    }

    private CanaryConfig getConfig(String filename) throws IOException {
        String contents = getFileContent(filename);
        configureObjectMapper(objectMapper);
        return objectMapper.readValue(contents, CanaryConfig.class);
    }

    private CanaryMetricConfigWithResults queryMetric(CanaryMetricConfig metric, OpentsdbCanaryScope scope) {
        Long step = 10L;
        Long start = scope.getStart().getEpochSecond() / step * step;
        Long end = scope.getEnd().getEpochSecond() / step * step;
        Long count = (end - start) / step;

        OpentsdbCanaryMetricSetQueryConfig opentsdbMetricSetQuery =
            (OpentsdbCanaryMetricSetQueryConfig) metric.getQuery();

        List<List<Double>> dataPoints = new LinkedList<>();
        LongStream.range(0, count).forEach(i -> {
            Long time = (start + i * step);
            dataPoints.add(Lists.newArrayList((double) i, time.doubleValue()));
        });

        List<List> values = new LinkedList<>();
        LongStream.range(0, count).forEach(i -> {
            List<Double> point = new LinkedList<>();
            point.add((double) i);
            point.add((double) i);
            values.add(point);
        });

        OpentsdbResults opentsdbResults = OpentsdbResults.builder()
            .metricName(opentsdbMetricSetQuery.getMetricName() + "." + scope.getScope())
            .dps(values).build();

        return CanaryMetricConfigWithResults.builder()
            .canaryMetricConfig(metric)
            .opentsdbResults(Collections.singletonList(opentsdbResults))
            .build();
    }

    @Test
    public void loadConfig() throws Exception {
        CanaryConfig config = getConfig("com/netflix/kayenta/controllers/sample-config.json");

        OpentsdbCanaryScope experiment = new OpentsdbCanaryScope();
        experiment.setStart(Instant.parse("2000-01-01T00:11:22Z"));
        experiment.setEnd(Instant.parse("2000-01-01T00:15:22Z"));
        experiment.setScope("staging");

        OpentsdbCanaryScope control = new OpentsdbCanaryScope();
        control.setStart(Instant.parse("2000-01-01T00:11:22Z"));
        control.setEnd(Instant.parse("2000-01-01T00:15:22Z"));
        control.setScope("prod");

        Map<CanaryMetricConfig, List<OpentsdbResults>> experimentMetrics = config.getMetrics().stream()
            .map((metric) -> queryMetric(metric, experiment))
            .collect(Collectors.toMap(CanaryMetricConfigWithResults::getCanaryMetricConfig,
                CanaryMetricConfigWithResults::getOpentsdbResults));

        Map<CanaryMetricConfig, List<OpentsdbResults>> controlMetrics = config.getMetrics().stream()
            .map((metric) -> queryMetric(metric, control))
            .collect(Collectors.toMap(CanaryMetricConfigWithResults::getCanaryMetricConfig,
                CanaryMetricConfigWithResults::getOpentsdbResults));

       /* OpentsdbResults experimentResult = Lists.newArrayList(experimentMetrics.values()).get(0).get(0);
        assertEquals(946685480L, experimentResult.getStart().longValue());
        assertEquals(10, experimentResult.getInterval().longValue());
        assertEquals(24, experimentResult.getDataPoints().collect(Collectors.toList()).size());*/
    }
}
