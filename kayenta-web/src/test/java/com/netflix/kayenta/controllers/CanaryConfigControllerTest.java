package com.netflix.kayenta.controllers;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.storage.ObjectType;
import com.netflix.kayenta.storage.StorageService;
import com.netflix.spinnaker.kork.web.exceptions.NotFoundException;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

public class CanaryConfigControllerTest extends BaseControllerTest {

  private static final String CONFIGS_ACCOUNT = "configs-account";
  private static final String CONFIG_ID = "canary_config_12345";
  private static final AccountCredentials CREDENTIALS = getCredentials(CONFIGS_ACCOUNT);
  private StorageService storageService = mock(StorageService.class);

  @Before
  public void setUp() {
    when(accountCredentialsRepository.getOne(CONFIGS_ACCOUNT)).thenReturn(Optional.of(CREDENTIALS));
    when(storageServiceRepository.getOne(CONFIGS_ACCOUNT)).thenReturn(Optional.of(storageService));
  }

  @Test
  public void getCanaryConfig_returnsOkResponseForExistingConfiguration() throws Exception {
    CanaryConfig response = CanaryConfig.builder().application("test-app").build();
    when(storageService.loadObject(CONFIGS_ACCOUNT, ObjectType.CANARY_CONFIG, CONFIG_ID))
        .thenReturn(response);

    this.mockMvc
        .perform(
            get(
                "/canaryConfig/{configId}?configurationAccountName={account}",
                CONFIG_ID,
                CONFIGS_ACCOUNT))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(jsonPath("$.applications.length()").value(is(1)))
        .andExpect(jsonPath("$.applications[0]").value(is("test-app")));
  }

  @Test
  public void getCanaryConfig_returnsNotFoundResponseForNotExistingConfiguration()
      throws Exception {
    when(storageService.loadObject(CONFIGS_ACCOUNT, ObjectType.CANARY_CONFIG, CONFIG_ID))
        .thenThrow(new NotFoundException("dummy message"));

    this.mockMvc
        .perform(
            get(
                "/canaryConfig/{configId}?configurationAccountName={account}",
                CONFIG_ID,
                CONFIGS_ACCOUNT))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(jsonPath("$.message").value(is("dummy message")));
  }

  @Test
  public void getCanaryConfig_returnsBadRequestResponseForNotResolvedAccount() throws Exception {
    when(accountCredentialsRepository.getOne(CONFIGS_ACCOUNT)).thenReturn(Optional.empty());

    this.mockMvc
        .perform(
            get(
                "/canaryConfig/{configId}?configurationAccountName={account}",
                CONFIG_ID,
                CONFIGS_ACCOUNT))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(
            jsonPath("$.message")
                .value(containsString("Unable to resolve account " + CONFIGS_ACCOUNT)));
  }

  @Test
  public void storeCanaryConfig_returnsBadRequestResponseForMissingRequestBody() throws Exception {
    this.mockMvc
        .perform(
            post("/canaryConfig?configurationAccountName={account}", CONFIGS_ACCOUNT)
                .contentType(APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(jsonPath("$.message").value(containsString("Required request body is missing")));
  }

  @Test
  public void deleteConfig_returnsNotContent() throws Exception {
    this.mockMvc
        .perform(
            delete(
                    "/canaryConfig/{configId}?configurationAccountName={account}",
                    CONFIG_ID,
                    CONFIGS_ACCOUNT)
                .contentType(APPLICATION_JSON))
        .andExpect(status().isNoContent());
  }

  private static AccountCredentials getCredentials(String accountName) {
    AccountCredentials credentials = mock(AccountCredentials.class);
    when(credentials.getName()).thenReturn(accountName);
    return credentials;
  }
}
