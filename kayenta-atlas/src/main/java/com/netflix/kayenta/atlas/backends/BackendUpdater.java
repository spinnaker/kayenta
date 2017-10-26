package com.netflix.kayenta.atlas.backends;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.kayenta.atlas.model.Backend;
import com.netflix.kayenta.atlas.service.BackendsRemoteService;
import com.netflix.kayenta.retrofit.config.RemoteService;
import com.netflix.kayenta.retrofit.config.RetrofitClientFactory;
import com.squareup.okhttp.OkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import retrofit.converter.JacksonConverter;

import java.util.List;

@Service
@Slf4j
@EnableConfigurationProperties
@ConditionalOnProperty("kayenta.atlas.enabled")
public class BackendUpdater {
  @Autowired RetrofitClientFactory retrofitClientFactory;
  @Autowired ObjectMapper objectMapper;

  @Scheduled(initialDelay = 0, fixedDelay=120000)
  public void run() {
    OkHttpClient okHttpClient = new OkHttpClient();

    log.info("Updating backends.json");
    RemoteService remoteService = new RemoteService();
    remoteService.setBaseUrl("http://insight-facts.prod.netflix.net");
    BackendsRemoteService backendsRemoteService = retrofitClientFactory.createClient(BackendsRemoteService.class,
                                                                                     new JacksonConverter(objectMapper),
                                                                                     remoteService,
                                                                                     okHttpClient);
    List<Backend> backends = backendsRemoteService.fetch();
    log.debug(backends.toString());
  }
}
