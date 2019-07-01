package com.netflix.kayenta.blobs.config;

import com.netflix.kayenta.azure.security.AzureNamedAccountCredentials;
import com.netflix.kayenta.blobs.storage.BlobsStorageService;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@Configuration
@EnableConfigurationProperties
@ConditionalOnProperty("kayenta.blobs.enabled")
@ComponentScan({"com.netflix.kayenta.blobs"})
@Slf4j
public class BlobsConfiguration {
    @Bean
    @DependsOn({"registerAzureCredentials"})
    public BlobsStorageService blobsStorageService(AccountCredentialsRepository accountCredentialsRepository) {
        BlobsStorageService.BlobsStorageServiceBuilder blobsStorageServiceBuilder = BlobsStorageService.builder();

        accountCredentialsRepository
                .getAll()
                .stream()
                .filter(c -> c instanceof AzureNamedAccountCredentials)
                .filter(c -> c.getSupportedTypes().contains(AccountCredentials.Type.OBJECT_STORE))
                .map(c -> c.getName())
                .forEach(blobsStorageServiceBuilder::accountName);

        BlobsStorageService blobsStorageService = blobsStorageServiceBuilder.build();

        log.info("Populated BlobsStorageService with {} Azure accounts.", blobsStorageService.getAccountNames().size());

        return blobsStorageService;
    }
}
