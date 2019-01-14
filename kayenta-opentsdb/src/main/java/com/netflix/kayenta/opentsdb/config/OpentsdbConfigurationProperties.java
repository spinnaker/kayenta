package com.netflix.kayenta.opentsdb.config;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class OpentsdbConfigurationProperties {

  @Getter
  private List<OpentsdbManagedAccount> accounts = new ArrayList<>();
}
