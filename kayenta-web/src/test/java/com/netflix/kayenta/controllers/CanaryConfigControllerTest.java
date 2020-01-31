package com.netflix.kayenta.controllers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.storage.ObjectType;
import com.netflix.spinnaker.kork.web.exceptions.NotFoundException;
import org.junit.Test;

public class CanaryConfigControllerTest extends BaseControllerTest {

  private static final String CONFIG_ID = "canary_config_12345";

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
    this.mockMvc
        .perform(
            get(
                "/canaryConfig/{configId}?configurationAccountName={account}",
                CONFIG_ID,
                "unknown-account"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.message", equalTo("Unable to resolve account unknown-account.")));
  }

  @Test
  public void storeCanaryConfig_returnsBadRequestResponseForMissingRequestBody() throws Exception {
    this.mockMvc
        .perform(
            post("/canaryConfig?configurationAccountName={account}", CONFIGS_ACCOUNT)
                .contentType(APPLICATION_JSON))
        .andExpect(status().isInternalServerError()); // expected: bad request ( should be fixed by:
    // https://github.com/spinnaker/kork/pull/535)
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
}
