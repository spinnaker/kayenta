package com.netflix.kayenta.opentsdb.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class OpentsdbResults {

  @NotNull
  @Getter
  private String metricName;

  /*
  @NotNull
  @Getter
  private long startTimeMillis;

  @NotNull
  @Getter
  @Setter
  private long stepSecs;

  @NotNull
  @Getter
  private long endTimeMillis;
  */

  @NotNull
  @Getter
  private Map<String, String> tags;

  /*
  @NotNull
  @Getter
  private List<Double> dataValues;
  */

  @NotNull
  @Getter
  private List<List> dps;

  @JsonIgnore
  public List<Double> getAdjustedDataValues(long stepSecs, long start, long end) {
    // Start at <start> time and index zero.
    log.info("getAdjustedDataValues with stepSecs={}, start={}, end={}, dps.size={}", stepSecs, start, end, this.dps.size());
    List<Double> adjustedPointList = new ArrayList<Double>();
    int idx = 0;
    for (Long time = start; time <= end; time += stepSecs) {
      log.info("time={}, idx={}", time, idx);
      if (idx < this.dps.size()) {
        List<Number> point = this.dps.get(idx);

        // Bring current index up to current timestamp
        while (point.get(0).longValue() < time && idx < this.dps.size() - 1) {
          idx++;
          point = this.dps.get(idx);
        }

        // If the point at this index matches the timestamp at this position,
        // add the value to the array and advance the index.
        if (point.get(0).longValue() == time) {
          adjustedPointList.add(point.get(1).doubleValue());
          idx++;
          continue;
        }
      }
      // Otherwise, put in a NaN to represent the "gap" in the OpenTSDB
      // data.
      adjustedPointList.add(Double.NaN);
    }

    return adjustedPointList;
  }
}
