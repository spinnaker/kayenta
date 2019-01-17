package com.netflix.kayenta.canary.providers.metrics;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.netflix.kayenta.canary.CanaryMetricSetQueryConfig;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.List;
import com.netflix.kayenta.opentsdb.metrics.TagPair;

@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("opentsdb")
public class OpentsdbCanaryMetricSetQueryConfig implements CanaryMetricSetQueryConfig {

  public static final String SERVICE_TYPE = "opentsdb";

  @NotNull
  @Getter
  private String metricName;

  @Getter
  private List<TagPair> tags;

  @Getter
  private String aggregator;

  @Getter
  private String downsample;

  @Getter
  private boolean rate;

  @Override
  public String getServiceType() {
    return SERVICE_TYPE;
  }
}
