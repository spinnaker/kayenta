package com.netflix.kayenta.standalonecanaryanalysis.event.listener;

import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.kayenta.standalonecanaryanalysis.event.StandaloneCanaryAnalysisExecutionCompletedEvent;
import com.netflix.kayenta.storage.ObjectType;
import com.netflix.kayenta.storage.StorageServiceRepository;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StandaloneCanaryAnalysisExecutionArchivalListener {

  private final AccountCredentialsRepository accountCredentialsRepository;
  private final StorageServiceRepository storageServiceRepository;

  @Autowired
  public StandaloneCanaryAnalysisExecutionArchivalListener(
      AccountCredentialsRepository accountCredentialsRepository,
      StorageServiceRepository storageServiceRepository) {
    this.accountCredentialsRepository = accountCredentialsRepository;
    this.storageServiceRepository = storageServiceRepository;
  }

  @EventListener
  public void onApplicationEvent(StandaloneCanaryAnalysisExecutionCompletedEvent event) {
    val response = event.getCanaryAnalysisExecutionStatusResponse();

    Optional.ofNullable(response.getStorageAccountName())
        .ifPresent(
            storageAccountName -> {
              val resolvedStorageAccountName =
                  accountCredentialsRepository
                      .getRequiredOneBy(storageAccountName, AccountCredentials.Type.OBJECT_STORE)
                      .getName();

              val storageService =
                  storageServiceRepository.getRequiredOne(resolvedStorageAccountName);

              storageService.storeObject(
                  resolvedStorageAccountName,
                  ObjectType.STANDALONE_CANARY_RESULT_ARCHIVE,
                  response.getPipelineId(),
                  response);
            });
  }
}
