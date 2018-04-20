package com.netflix.kayenta.wavefront.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.netflix.kayenta.wavefront.service.WavefrontRemoteService;
import com.netflix.kayenta.retrofit.config.RemoteService;
import com.netflix.kayenta.security.AccountCredentials;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import javax.validation.constraints.NotNull;
import java.util.List;

@Builder
@Data
public class WavefrontNamedAccountCredentials implements AccountCredentials<WavefrontCredentials> {

    @NotNull
    private String name;

    @NotNull
    @Singular
    private List<Type> supportedTypes;

    @NotNull
    private WavefrontCredentials credentials;

    @NotNull
    private RemoteService endpoint;

    @Override
    public String getType() {
        return "wavefront";
    }

    @JsonIgnore
    WavefrontRemoteService wavefrontRemoteService;
}
