package com.netflix.kayenta.atlas.backends;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BackendsUpdater {

  @Scheduled(initialDelay = 0, fixedDelay=120000)
  public void run() {
    log.info("Updating backends.json");
  }
}
