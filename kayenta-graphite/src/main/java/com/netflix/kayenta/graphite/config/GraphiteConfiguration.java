package com.netflix.kayenta.graphite.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.kayenta.graphite.metrics.GraphiteMetricsService;
import com.netflix.kayenta.graphite.security.GraphiteCredentials;
import com.netflix.kayenta.graphite.security.GraphiteNamedAccountCredentials;
import com.netflix.kayenta.graphite.service.GraphiteRemoteService;
import com.netflix.kayenta.metrics.MetricsService;
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
import retrofit.converter.JacksonConverter;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableConfigurationProperties
@ConditionalOnProperty("kayenta.graphite.enabled")
@ComponentScan({"com.netflix.kayenta.graphite"})
@Slf4j
public class GraphiteConfiguration {
    @Bean
    @ConfigurationProperties("kayenta.graphite")
    GraphiteConfigurationProperties graphiteConfigurationProperties() {
        return new GraphiteConfigurationProperties();
    }

    @Bean
    @ConfigurationProperties("kayenta.graphite.testControllerDefaults")
    GraphiteConfigurationTestControllerDefaultProperties graphiteConfigurationTestControllerDefaultProperties() {
        return new GraphiteConfigurationTestControllerDefaultProperties();
    }

    @Bean
    MetricsService graphiteMetricsService(GraphiteConfigurationProperties graphiteConfigurationProperties,
                                          RetrofitClientFactory retrofitClientFactory,
                                          ObjectMapper objectMapper,
                                          OkHttpClient okHttpClient,
                                          AccountCredentialsRepository accountCredentialsRepository) throws IOException {
        GraphiteMetricsService.GraphiteMetricsServiceBuilder graphiteMetricsServiceBuilder =
                GraphiteMetricsService.builder();

        for (GraphiteManagedAccount account : graphiteConfigurationProperties.getAccounts()) {
            String accountName = account.getName();
            List<AccountCredentials.Type> supportedTypes = account.getSupportedTypes();

            GraphiteCredentials credentials = GraphiteCredentials.builder().build();

            GraphiteNamedAccountCredentials.GraphiteNamedAccountCredentialsBuilder
                    accountCredentialsBuilder =
                    GraphiteNamedAccountCredentials
                            .builder()
                            .name(accountName)
                            .endpoint(account.getEndpoint())
                            .credentials(credentials);
            if (!CollectionUtils.isEmpty(supportedTypes)) {
                if (supportedTypes.contains(AccountCredentials.Type.METRICS_STORE)) {
                    accountCredentialsBuilder
                            .graphiteRemoteService(
                                    retrofitClientFactory.createClient(GraphiteRemoteService.class,
                                            new JacksonConverter(objectMapper), account.getEndpoint(), okHttpClient));
                }

                accountCredentialsBuilder.supportedTypes(supportedTypes);
            }

            accountCredentialsRepository.save(accountName, accountCredentialsBuilder.build());
            graphiteMetricsServiceBuilder.accountName(accountName);

        }

        log.info("Populated GraphiteMetricsService with {} Graphite accounts.",
                graphiteConfigurationProperties.getAccounts().size());
        return graphiteMetricsServiceBuilder.build();
    }
}
