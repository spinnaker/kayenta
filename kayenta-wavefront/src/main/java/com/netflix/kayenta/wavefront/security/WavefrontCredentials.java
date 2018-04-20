package com.netflix.kayenta.wavefront.security;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Builder
@Data
@Slf4j
public class WavefrontCredentials {

    private static String applicationVersion =
            Optional.ofNullable(WavefrontCredentials.class.getPackage().getImplementationVersion()).orElse("Unknown");

    @NonNull
    private String apiToken;

    public String getApiToken() {
        return "Bearer " + apiToken;
    }
}
