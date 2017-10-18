package com.netflix.kayenta.atlas.backends;

import com.netflix.kayenta.atlas.model.Backend;
import com.netflix.kayenta.atlas.service.BackendsRemoteService;
import com.netflix.kayenta.retrofit.config.RemoteService;
import com.netflix.kayenta.retrofit.config.RetrofitClientFactory;
import com.netflix.kayenta.util.ObjectMapperFactory;
import com.squareup.okhttp.OkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import retrofit.converter.JacksonConverter;

import java.util.List;

@Service
@Slf4j
@EnableConfigurationProperties
@ConditionalOnProperty("kayenta.atlas.enabled")
public class BackendUpdater {

  @Scheduled(initialDelay = 0, fixedDelay=120000)
  public void run() {
    // TODO: (mgraff) clean this up, we should not be making these here even if this works...
    RetrofitClientFactory retrofitClientFactory = new RetrofitClientFactory();
    OkHttpClient okHttpClient = new OkHttpClient();

    log.info("Updating backends.json");
    RemoteService remoteService = new RemoteService();
    remoteService.setBaseUrl("http://uicommons.prod.netflix.net/data/backends.json");
    BackendsRemoteService backendsRemoteService = retrofitClientFactory.createClient(BackendsRemoteService.class,
                                                                                     null,
                                                                                     remoteService,
                                                                                     okHttpClient);
    List<Backend> backends = backendsRemoteService.fetch();
    log.debug(backends.toString());
  }
}
