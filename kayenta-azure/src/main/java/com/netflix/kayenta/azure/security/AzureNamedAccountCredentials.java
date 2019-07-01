package com.netflix.kayenta.azure.security;


import com.microsoft.azure.storage.blob.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.netflix.kayenta.security.AccountCredentials;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import javax.validation.constraints.NotNull;
import java.util.List;

@Builder
@Data
public class AzureNamedAccountCredentials implements AccountCredentials<AzureCredentials> {

    @NotNull
    private String name;

    @NotNull
    @Singular
    private List<Type> supportedTypes;

    @NotNull
    private AzureCredentials credentials;

    private String rootFolder;

    @Override
    public String getType() {
        return "azure";
    }

    @JsonIgnore
    private CloudBlobContainer azureContainer;
}

