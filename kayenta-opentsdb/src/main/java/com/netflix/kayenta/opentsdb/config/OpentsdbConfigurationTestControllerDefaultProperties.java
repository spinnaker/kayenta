package com.netflix.kayenta.opentsdb.config;

import lombok.Getter;
import lombok.Setter;

/**
 * This configuration class allows you to specify default values for the OpenTsdbFetchController.
 */
public class OpentsdbConfigurationTestControllerDefaultProperties {

  @Getter
  @Setter
  private String project;

  @Getter
  @Setter
  private String resourceType;

  @Getter
  @Setter
  private String location;

  @Getter
  @Setter
  private String scope;

  @Getter
  @Setter
  private String start;

  @Getter
  @Setter
  private String end;
}
