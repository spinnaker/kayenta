package com.netflix.kayenta.wavefront.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Data
public class WavefrontTimeSeries {
    private String name;
    private String query;
    private String warnings;
    private Map<String, Integer> stats;
    private Map<String, Object> events;
    @JsonProperty("timeseries")
    private List<WavefrontSeriesEntry> timeSeries;
    private Long granularity;

    @Data
    static public class WavefrontSeriesEntry {
        private String label;
        private String host;
        private Map<String, String> tags;
        private List<List<Number>> data;


        // Wavefront returns an array of timestamp/value pairs; the pairs are
        // ordered, but may not be sequential (ie. may be a sparse result)
        // Since Kayenta's MetricSet is storing a simple array, we need to
        // convert this sparse list to a full array, and make sure we slot the
        // values into the correct array indices.
        @JsonIgnore
        private List<Double> adjustedPointList;
        @JsonIgnore
        private List<Double> getAdjustedPointList(long step) {
            if ((this.adjustedPointList != null) && (this.adjustedPointList.size() != 0)) {
                // Already computed, just return.
                return this.adjustedPointList;
            }

            this.adjustedPointList = new ArrayList<Double>();
            List<Number> firstPoint = data.get(0);
            List<Number> lastPoint = data.get(data.size() - 1);


            // Start at <start> time and index zero.
            Long startTime = firstPoint.get(0).longValue();
            int idx = 0;
            for (Long time = startTime; time <= lastPoint.get(0).longValue(); time += step) {
                    List<Number> point = data.get(idx);
                    while (point.get(0).longValue() < time && data.size() - 1 != idx) {
                        idx++;
                        point = data.get(idx);
                    }
                    if (point.get(0).longValue() == time) {
                        this.adjustedPointList.add(point.get(1).doubleValue());
                    } else {
                        this.adjustedPointList.add(Double.NaN);
                    }
            }

            return this.adjustedPointList;
        }

        @JsonIgnore
        public Stream<Double> getDataPoints(long step) {
            return this.getAdjustedPointList(step).stream();
        }

    }
}
