package com.netflix.kayenta.azure.config;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class AzureConfigurationProperties {

  @Getter
  private List<AzureManagedAccount> accounts = new ArrayList<>();
}
