package com.netflix.kayenta.opentsdb.metrics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TagPair {

  private String key;
  private String value;
}
