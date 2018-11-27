package com.netflix.kayenta.graphite.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;


@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
@Data
public class GraphiteMetricDescriptorsResponse {

    private List<GraphiteMetricDescriptorResponseEntity> metrics;

    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Slf4j
    @Data
    public static class GraphiteMetricDescriptorResponseEntity {
        private String path;

        private String name;

        private String isLeaf;
    }
}
