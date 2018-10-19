package com.netflix.kayenta.graphite.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@Data
public class GraphiteResults {
    private String target;
    private List<List<Double>> datapoints;

    @JsonIgnore
    private List<Double> adjustedPointList;

    @JsonIgnore
    private List<Double> getAdjustedPointList() {
        if (!CollectionUtils.isEmpty(adjustedPointList)) {
            return this.adjustedPointList;
        } else {
            this.adjustedPointList = new ArrayList<>();
            for (List<Double> point : this.datapoints) {
                if (point.get(0) == null) {
                    this.adjustedPointList.add(Double.NaN);
                } else {
                    this.adjustedPointList.add(point.get(0));
                }
            }
        }
        return this.adjustedPointList;
    }

    @JsonIgnore
    public Stream<Double> getDataPoints() {
        return this.getAdjustedPointList().stream();
    }

    @JsonIgnore
    public Long getInterval() {
        if (datapoints.size() >= 2) {
            return (long)(datapoints.get(1).get(1) - datapoints.get(0).get(1));
        } else {
            return 1L;
        }
    }

    @JsonIgnore
    public Long getStart() {
        if (!CollectionUtils.isEmpty(this.datapoints)) {
            return this.datapoints.get(0).get(1).longValue();
        } else {
            return 0L;
        }
    }

    @JsonIgnore
    public Long getEnd() {
        if (!CollectionUtils.isEmpty(this.datapoints)) {
            return this.datapoints.get(this.datapoints.size() - 1).get(1).longValue();
        } else {
            return 0L;
        }
    }
}
