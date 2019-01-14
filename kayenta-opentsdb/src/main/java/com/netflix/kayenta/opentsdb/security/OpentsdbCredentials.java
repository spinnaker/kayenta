package com.netflix.kayenta.opentsdb.security;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Builder
@Data
@Slf4j
// TODO(ewisbelatt): Not sure what kind of credentials or configuration is really required here yet.
//       OpenTsdb does not have security. To secure it, you front it with a webserver that adds security (e.g. basic auth).
//       So this is likely just a username/password.
public class OpentsdbCredentials {

  private static String applicationVersion =
    Optional.ofNullable(OpentsdbCredentials.class.getPackage().getImplementationVersion()).orElse("Unknown");
}
