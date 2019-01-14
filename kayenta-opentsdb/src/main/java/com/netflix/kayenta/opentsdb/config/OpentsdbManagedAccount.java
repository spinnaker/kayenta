package com.netflix.kayenta.opentsdb.config;

import com.netflix.kayenta.retrofit.config.RemoteService;
import com.netflix.kayenta.security.AccountCredentials;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class OpentsdbManagedAccount {

  @NotNull
  private String name;

  // Location of opentsdb server.
  @NotNull
  @Getter
  @Setter
  private RemoteService endpoint;

  private List<AccountCredentials.Type> supportedTypes;
}
