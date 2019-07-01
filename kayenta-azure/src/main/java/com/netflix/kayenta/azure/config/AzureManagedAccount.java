package com.netflix.kayenta.azure.config;

import com.netflix.kayenta.security.AccountCredentials;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class AzureManagedAccount {

    @NotNull
    private String name;
  
    @NotNull
    private String storageAccountName;

    @NotNull
    private String accountAccessKey;

    @NotNull
    private String endpointSuffix;
  
    private String container;
    private String rootFolder;
  
    private List<AccountCredentials.Type> supportedTypes;
}
