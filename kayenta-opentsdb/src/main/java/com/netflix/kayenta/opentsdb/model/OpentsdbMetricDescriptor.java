package com.netflix.kayenta.opentsdb.model;

import java.util.Collections;
import java.util.Map;

public class OpentsdbMetricDescriptor {

  private final String name;
  private final Map<String, String> map;

  public OpentsdbMetricDescriptor(String name) {
    this.name = name;
    this.map = Collections.singletonMap("name", name);
  }

  public String getName() {
    return name;
  }

  // This is so we can efficiently serialize to json without having to construct the intermediate map object on each query.
  public Map<String, String> getMap() {
    return map;
  }
}
