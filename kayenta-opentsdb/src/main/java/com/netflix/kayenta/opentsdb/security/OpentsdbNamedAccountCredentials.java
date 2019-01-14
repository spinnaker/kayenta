package com.netflix.kayenta.opentsdb.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.netflix.kayenta.opentsdb.service.OpentsdbRemoteService;
import com.netflix.kayenta.retrofit.config.RemoteService;
import com.netflix.kayenta.security.AccountCredentials;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import javax.validation.constraints.NotNull;
import java.util.List;

@Builder
@Data
public class OpentsdbNamedAccountCredentials implements AccountCredentials<OpentsdbCredentials> {

  @NotNull
  private String name;

  @NotNull
  @Singular
  private List<Type> supportedTypes;

  @NotNull
  private OpentsdbCredentials credentials;

  // The OpenTSDB server location.
  @NotNull
  private RemoteService endpoint;

  @Override
  public String getType() {
    return "opentsdb";
  }

  @JsonIgnore
  OpentsdbRemoteService opentsdbRemoteService;
}
