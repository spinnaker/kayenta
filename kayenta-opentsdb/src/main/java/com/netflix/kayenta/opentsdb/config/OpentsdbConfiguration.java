package com.netflix.kayenta.opentsdb.config;

import com.netflix.kayenta.metrics.MetricsService;
import com.netflix.kayenta.opentsdb.metrics.OpentsdbMetricsService;
import com.netflix.kayenta.opentsdb.config.OpentsdbResponseConverter;
import com.netflix.kayenta.opentsdb.security.OpentsdbCredentials;
import com.netflix.kayenta.opentsdb.security.OpentsdbNamedAccountCredentials;
import com.netflix.kayenta.opentsdb.service.OpentsdbRemoteService;
import com.netflix.kayenta.retrofit.config.RetrofitClientFactory;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.squareup.okhttp.OkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableConfigurationProperties
@ConditionalOnProperty("kayenta.opentsdb.enabled")
@ComponentScan({"com.netflix.kayenta.opentsdb"})
@Slf4j
public class OpentsdbConfiguration {

  @Bean
  @ConfigurationProperties("kayenta.opentsdb")
  OpentsdbConfigurationProperties opentsdbConfigurationProperties() {
    return new OpentsdbConfigurationProperties();
  }

  @Bean
  @ConfigurationProperties("kayenta.opentsdb.testControllerDefaults")
  OpentsdbConfigurationTestControllerDefaultProperties opentsdbConfigurationTestControllerDefaultProperties() {
    return new OpentsdbConfigurationTestControllerDefaultProperties();
  }

  @Bean
  MetricsService opentsdbMetricsService(OpentsdbResponseConverter opentsdbConverter,
                                          OpentsdbConfigurationProperties opentsdbConfigurationProperties,
                                          RetrofitClientFactory retrofitClientFactory,
                                          OkHttpClient okHttpClient,
                                          AccountCredentialsRepository accountCredentialsRepository) throws IOException {
    OpentsdbMetricsService.OpentsdbMetricsServiceBuilder opentsdbMetricsServiceBuilder = OpentsdbMetricsService.builder();
    // opentsdbMetricsServiceBuilder.scopeLabel(opentsdbConfigurationProperties.getScopeLabel());

    for (OpentsdbManagedAccount opentsdbManagedAccount : opentsdbConfigurationProperties.getAccounts()) {
      String name = opentsdbManagedAccount.getName();
      List<AccountCredentials.Type> supportedTypes = opentsdbManagedAccount.getSupportedTypes();

      log.info("Registering Opentsdb account {} with supported types {}.", name, supportedTypes);

      OpentsdbCredentials opentsdbCredentials =
        OpentsdbCredentials
          .builder()
          .build();
      OpentsdbNamedAccountCredentials.OpentsdbNamedAccountCredentialsBuilder opentsdbNamedAccountCredentialsBuilder =
        OpentsdbNamedAccountCredentials
          .builder()
          .name(name)
          .endpoint(opentsdbManagedAccount.getEndpoint())
          .credentials(opentsdbCredentials);

      if (!CollectionUtils.isEmpty(supportedTypes)) {
        if (supportedTypes.contains(AccountCredentials.Type.METRICS_STORE)) {
          OpentsdbRemoteService opentsdbRemoteService = retrofitClientFactory.createClient(OpentsdbRemoteService.class,
                                                                                               opentsdbConverter,
                                                                                               opentsdbManagedAccount.getEndpoint(),
                                                                                               okHttpClient);

          opentsdbNamedAccountCredentialsBuilder.opentsdbRemoteService(opentsdbRemoteService);
        }

        opentsdbNamedAccountCredentialsBuilder.supportedTypes(supportedTypes);
      }

      OpentsdbNamedAccountCredentials opentsdbNamedAccountCredentials = opentsdbNamedAccountCredentialsBuilder.build();
      accountCredentialsRepository.save(name, opentsdbNamedAccountCredentials);
      opentsdbMetricsServiceBuilder.accountName(name);
    }

    OpentsdbMetricsService opentsdbMetricsService = opentsdbMetricsServiceBuilder.build();

    log.info("Populated OpentsdbMetricsService with {} Opentsdb accounts.", opentsdbMetricsService.getAccountNames().size());

    return opentsdbMetricsService;
  }
}
