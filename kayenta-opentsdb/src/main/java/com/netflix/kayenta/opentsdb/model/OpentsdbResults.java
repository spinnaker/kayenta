package com.netflix.kayenta.opentsdb.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
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
    List<Double> adjustedPointList = new ArrayList<Double>();
    int idx = 0;
    for (Long time = start; time <= end; time += stepSecs) {
      List<Number> point = this.dps.get(idx);

      // If the point at this index matches the timestamp at this position,
      // add the value to the array and advance the index.
      if (point.get(0).longValue() == time) {
        adjustedPointList.add(point.get(1).doubleValue());
        idx++;
      } else {
        // Otherwise, put in a NaN to represent the "gap" in the OpenTSDB
        // data.
        adjustedPointList.add(Double.NaN);
      }
    }

    return adjustedPointList;
  }
}
