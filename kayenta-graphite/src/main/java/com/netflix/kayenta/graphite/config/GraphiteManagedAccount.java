package com.netflix.kayenta.graphite.config;

import com.netflix.kayenta.retrofit.config.RemoteService;
import com.netflix.kayenta.security.AccountCredentials;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class GraphiteManagedAccount {
    @NotNull
    private String name;

    @NotNull
    private RemoteService endpoint;

    private List<AccountCredentials.Type> supportedTypes;
}
