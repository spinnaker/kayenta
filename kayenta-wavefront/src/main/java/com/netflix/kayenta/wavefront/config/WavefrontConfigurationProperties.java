package com.netflix.kayenta.wavefront.config;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class WavefrontConfigurationProperties {
    @Getter
    private List<WavefrontManagedAccount> accounts = new ArrayList<>();
}
