package com.netflix.kayenta.graphite.security;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Builder
@Data
@Slf4j
public class GraphiteCredentials {
    private static String applicationVersion =
            Optional.ofNullable(GraphiteCredentials
                    .class.getPackage().getImplementationVersion()).orElse("Unknown");
}
