/*
 * Copyright 2018 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.kayenta.canary;

import static com.netflix.spinnaker.orca.api.pipeline.models.ExecutionType.PIPELINE;
import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.netflix.kayenta.canary.orca.CanaryStageNames;
import com.netflix.kayenta.canary.providers.metrics.QueryConfigUtils;
import com.netflix.kayenta.canary.results.CanaryJudgeResult;
import com.netflix.kayenta.canary.results.CanaryResult;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;
import com.netflix.spinnaker.orca.api.pipeline.models.ExecutionStatus;
import com.netflix.spinnaker.orca.api.pipeline.models.PipelineExecution;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import com.netflix.spinnaker.orca.pipeline.ExecutionLauncher;
import com.netflix.spinnaker.orca.pipeline.model.PipelineExecutionImpl;
import com.netflix.spinnaker.orca.pipeline.model.StageExecutionImpl;
import com.netflix.spinnaker.orca.pipeline.persistence.ExecutionRepository;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class ExecutionMapper {

  public static final String PIPELINE_NAME = "Standard Canary Pipeline";
  public static final String ANONYMOUS_USER = "[anonymous]";

  private final ObjectMapper objectMapper;
  private final Registry registry;
  private final String currentInstanceId;
  private final List<CanaryScopeFactory> canaryScopeFactories;
  private final ExecutionLauncher executionLauncher;
  private final ExecutionRepository executionRepository;

  private final Id pipelineRunId;
  private final Id failureId;

  public ExecutionMapper(
      ObjectMapper objectMapper,
      Registry registry,
      String currentInstanceId,
      Optional<List<CanaryScopeFactory>> canaryScopeFactories,
      ExecutionLauncher executionLauncher,
      ExecutionRepository executionRepository) {
    this.objectMapper = objectMapper;
    this.registry = registry;
    this.currentInstanceId = currentInstanceId;
    this.canaryScopeFactories = canaryScopeFactories.orElseGet(Collections::emptyList);
    this.executionLauncher = executionLauncher;
    this.executionRepository = executionRepository;

    this.pipelineRunId = registry.createId("canary.pipelines.initiated");
    this.failureId = registry.createId("canary.pipelines.startupFailed");
  }

  /**
   * Maps the Orca Pipeline Execution for a canary triggered via the /canary endpoint to the status
   * response object.
   *
   * @param pipeline The Orca canary pipeline execution for the canary.
   * @return the canary execution status response.
   */
  public CanaryExecutionStatusResponse fromExecution(PipelineExecution pipeline) {
    if (!PIPELINE_NAME.equals(pipeline.getName())) {
      throw new IllegalArgumentException(
          String.format(
              "Only named pipelines of '%s' can be converted to a CanaryExecutionStatusResponse object, '%s' received",
              PIPELINE_NAME, pipeline.getName()));
    }

    StageExecution judgeStage = getStageFromExecution(pipeline, CanaryStageNames.REFID_JUDGE);
    Map<String, Object> judgeOutputs = judgeStage.getOutputs();

    StageExecution contextStage =
        getStageFromExecution(pipeline, CanaryStageNames.REFID_SET_CONTEXT);
    Map<String, Object> contextContext = contextStage.getContext();

    StageExecution mixerStage = getStageFromExecution(pipeline, CanaryStageNames.REFID_MIX_METRICS);
    Map<String, Object> mixerOutputs = mixerStage.getOutputs();

    CanaryExecutionStatusResponse.CanaryExecutionStatusResponseBuilder
        canaryExecutionStatusResponseBuilder =
            CanaryExecutionStatusResponse.builder()
                .application((String) contextContext.get("application"))
                .parentPipelineExecutionId((String) contextContext.get("parentPipelineExecutionId"))
                .pipelineId(pipeline.getId());

    ofNullable(contextContext.get("metricsAccountName"))
        .map(String::valueOf)
        .ifPresent(canaryExecutionStatusResponseBuilder::metricsAccountName);

    ofNullable(contextContext.get("storageAccountName"))
        .map(String::valueOf)
        .ifPresent(canaryExecutionStatusResponseBuilder::storageAccountName);

    ofNullable(contextContext.get("canaryConfigId"))
        .map(String::valueOf)
        .ifPresent(canaryExecutionStatusResponseBuilder::canaryConfigId);

    ofNullable(contextContext.get("configurationAccountName"))
        .map(String::valueOf)
        .ifPresent(canaryExecutionStatusResponseBuilder::configurationAccountName);

    ofNullable(mixerOutputs.get("metricSetPairListId"))
        .map(String::valueOf)
        .ifPresent(canaryExecutionStatusResponseBuilder::metricSetPairListId);

    canaryExecutionStatusResponseBuilder.config(getCanaryConfig(pipeline));
    CanaryExecutionRequest canaryExecutionRequest = getCanaryExecutionRequest(pipeline);
    canaryExecutionStatusResponseBuilder.canaryExecutionRequest(canaryExecutionRequest);

    Map<String, String> stageStatus =
        pipeline.getStages().stream()
            .collect(
                Collectors.toMap(
                    StageExecution::getRefId, s -> s.getStatus().toString().toLowerCase()));

    Boolean isComplete = pipeline.getStatus().isComplete();
    String pipelineStatus = pipeline.getStatus().toString().toLowerCase();

    canaryExecutionStatusResponseBuilder
        .stageStatus(stageStatus)
        .complete(isComplete)
        .status(pipelineStatus);

    ofNullable(pipeline.getBuildTime())
        .ifPresent(
            buildTime ->
                canaryExecutionStatusResponseBuilder
                    .buildTimeMillis(buildTime)
                    .buildTimeIso(Instant.ofEpochMilli(buildTime) + ""));

    ofNullable(pipeline.getStartTime())
        .ifPresent(
            startTime ->
                canaryExecutionStatusResponseBuilder
                    .startTimeMillis(startTime)
                    .startTimeIso(Instant.ofEpochMilli(startTime) + ""));

    ofNullable(pipeline.getEndTime())
        .ifPresent(
            endTime ->
                canaryExecutionStatusResponseBuilder
                    .endTimeMillis(endTime)
                    .endTimeIso(Instant.ofEpochMilli(endTime) + ""));

    if (isComplete && pipelineStatus.equals("succeeded")) {
      if (judgeOutputs.containsKey("result")) {
        Map<String, Object> resultMap = (Map<String, Object>) judgeOutputs.get("result");
        CanaryJudgeResult canaryJudgeResult =
            objectMapper.convertValue(resultMap, CanaryJudgeResult.class);
        Duration canaryDuration =
            canaryExecutionRequest != null ? canaryExecutionRequest.calculateDuration() : null;
        CanaryResult result =
            CanaryResult.builder()
                .judgeResult(canaryJudgeResult)
                .canaryDuration(canaryDuration)
                .build();
        canaryExecutionStatusResponseBuilder.result(result);
      }
    }

    // Propagate the first canary pipeline exception we can locate.
    StageExecution stageWithException =
        pipeline.getStages().stream()
            .filter(stage -> stage.getContext().containsKey("exception"))
            .findFirst()
            .orElse(null);

    if (stageWithException != null) {
      canaryExecutionStatusResponseBuilder.exception(
          stageWithException.getContext().get("exception"));
    }

    return canaryExecutionStatusResponseBuilder.build();
  }

  // Some older (stored) results have the execution request only in the judge context.
  public String getCanaryExecutionRequestFromJudgeContext(PipelineExecution pipeline) {
    StageExecution contextStage = getStageFromExecution(pipeline, CanaryStageNames.REFID_JUDGE);
    Map<String, Object> context = contextStage.getContext();

    return (String) context.get("canaryExecutionRequest");
  }

  public CanaryExecutionRequest getCanaryExecutionRequest(PipelineExecution pipeline) {
    StageExecution contextStage =
        getStageFromExecution(pipeline, CanaryStageNames.REFID_SET_CONTEXT);
    Map<String, Object> context = contextStage.getContext();

    String canaryExecutionRequestJSON = (String) context.get("canaryExecutionRequest");
    if (canaryExecutionRequestJSON == null) {
      canaryExecutionRequestJSON = getCanaryExecutionRequestFromJudgeContext(pipeline);
    }
    if (canaryExecutionRequestJSON == null) {
      return null;
    }
    CanaryExecutionRequest canaryExecutionRequest = null;
    try {
      canaryExecutionRequest =
          objectMapper.readValue(canaryExecutionRequestJSON, CanaryExecutionRequest.class);
    } catch (IOException e) {
      log.error("Cannot deserialize canaryExecutionRequest", e);
      throw new IllegalArgumentException("Cannot deserialize canaryExecutionRequest", e);
    }
    return canaryExecutionRequest;
  }

  public CanaryConfig getCanaryConfig(PipelineExecution pipeline) {
    StageExecution contextStage =
        getStageFromExecution(pipeline, CanaryStageNames.REFID_SET_CONTEXT);
    Map<String, Object> context = contextStage.getContext();

    Map<String, Object> canaryConfigMap = (Map<String, Object>) context.get("canaryConfig");
    return objectMapper.convertValue(canaryConfigMap, CanaryConfig.class);
  }

  /**
   * Fetches a StageExecution via its ref id from the Orca pipeline execution.
   *
   * @param pipeline The Orca pipeline Execution
   * @param refId The reference id for the stage
   * @return The stage.
   */
  protected StageExecution getStageFromExecution(PipelineExecution pipeline, String refId) {
    String canaryExecutionId = pipeline.getId();
    return pipeline.getStages().stream()
        .filter(stage -> refId.equals(stage.getRefId()))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format(
                        "Unable to find StageExecution '%s' in pipeline ID '%s'",
                        refId, canaryExecutionId)));
  }

  private CanaryScopeFactory getScopeFactoryForServiceType(String serviceType) {
    return canaryScopeFactories.stream()
        .filter((f) -> f.handles(serviceType))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Unable to resolve canary scope factory for '" + serviceType + "'."));
  }

  private CanaryScope getScopeForNamedScope(
      CanaryExecutionRequest executionRequest, String scopeName, boolean isCanary) {
    CanaryScopePair canaryScopePair = executionRequest.getScopes().get(scopeName);
    CanaryScope canaryScope =
        isCanary ? canaryScopePair.getExperimentScope() : canaryScopePair.getControlScope();
    if (canaryScope == null) {
      throw new IllegalArgumentException(
          "Canary scope for named scope "
              + scopeName
              + " is missing experimentScope or controlScope keys");
    }
    return canaryScope;
  }

  protected List<Map<String, Object>> generateFetchScopes(
      CanaryConfig canaryConfig,
      CanaryExecutionRequest executionRequest,
      boolean isCanary,
      String resolvedMetricsAccountName,
      String resolvedStorageAccountName) {
    return IntStream.range(0, canaryConfig.getMetrics().size())
        .mapToObj(
            index -> {
              CanaryMetricConfig metric = canaryConfig.getMetrics().get(index);
              String serviceType = metric.getQuery().getServiceType();
              CanaryScopeFactory canaryScopeFactory = getScopeFactoryForServiceType(serviceType);
              if (metric.getScopeName() == null) {
                throw new IllegalArgumentException(
                    "Canary scope for metric named '" + metric.getName() + "' is null.");
              }
              CanaryScope inspecificScope =
                  getScopeForNamedScope(executionRequest, metric.getScopeName(), isCanary);
              CanaryScope scopeModel = canaryScopeFactory.buildCanaryScope(inspecificScope);
              String stagePrefix =
                  (isCanary
                      ? CanaryStageNames.REFID_FETCH_EXPERIMENT_PREFIX
                      : CanaryStageNames.REFID_FETCH_CONTROL_PREFIX);
              String scopeJson;
              try {
                scopeJson = objectMapper.writeValueAsString(scopeModel);
              } catch (JsonProcessingException e) {
                throw new IllegalArgumentException(
                    "Cannot render scope to json"); // TODO: this seems like cheating
              }

              String currentStageId = stagePrefix + index;
              String previousStageId =
                  (index == 0) ? CanaryStageNames.REFID_SET_CONTEXT : stagePrefix + (index - 1);

              return new HashMap<>(
                  Map.of(
                      "refId", currentStageId,
                      "metricIndex", index,
                      "requisiteStageRefIds", List.of(previousStageId),
                      "user", ANONYMOUS_USER,
                      "metricsAccountName", resolvedMetricsAccountName,
                      "storageAccountName", resolvedStorageAccountName,
                      "stageType", serviceType + "Fetch",
                      "canaryScope", scopeJson));
            })
        .collect(Collectors.toList());
  }

  public CanaryExecutionResponse buildExecution(
      String application,
      String parentPipelineExecutionId,
      @NotNull String canaryConfigId,
      @NotNull CanaryConfig canaryConfig,
      String resolvedConfigurationAccountName,
      @NotNull String resolvedMetricsAccountName,
      @NotNull String resolvedStorageAccountName,
      @NotNull CanaryExecutionRequest canaryExecutionRequest)
      throws JsonProcessingException {

    registry
        .counter(
            pipelineRunId
                .withTag("canaryConfigId", canaryConfigId)
                .withTag("canaryConfigName", canaryConfig.getName()))
        .increment();

    if (StringUtils.isEmpty(application)) {
      application = "kayenta-" + currentInstanceId;
    }

    validateCanaryExecutionConfiguration(canaryConfig, canaryExecutionRequest);
    canaryConfig = QueryConfigUtils.escapeTemplates(canaryConfig);

    String canaryPipelineConfigId = application + "-standard-canary-pipeline";

    var pipeline = new PipelineExecutionImpl(PIPELINE, application);

    pipeline.setAuthentication(getAuthenticationDetails());
    pipeline.setName(PIPELINE_NAME);
    pipeline.setPipelineConfigId(canaryPipelineConfigId);
    pipeline.setLimitConcurrent(false);

    var setupCanaryContext =
        configureSetupCanaryContext(
            application,
            parentPipelineExecutionId,
            canaryConfigId,
            canaryConfig,
            resolvedConfigurationAccountName,
            resolvedMetricsAccountName,
            resolvedStorageAccountName);
    var fetchExperimentContexts =
        generateFetchScopes(
            canaryConfig,
            canaryExecutionRequest,
            true,
            resolvedMetricsAccountName,
            resolvedStorageAccountName);

    List<Map<String, Object>> controlFetchContexts =
        generateFetchScopes(
            canaryConfig,
            canaryExecutionRequest,
            false,
            resolvedMetricsAccountName,
            resolvedStorageAccountName);

    var mixMetricSetsContext =
        configureMixMetricSetsContext(canaryConfig, resolvedStorageAccountName);
    var orchestratorScoreThresholds = canaryExecutionRequest.getThresholds();
    var canaryJudgeContext =
        configureCanaryJudgeContext(
            resolvedStorageAccountName,
            canaryExecutionRequest,
            setupCanaryContext,
            orchestratorScoreThresholds);

    addStage(pipeline, "setupCanary", "Setup Canary", setupCanaryContext);
    addStage(
        pipeline, "metricSetMixer", "Mix Control and Experiment Results", mixMetricSetsContext);
    addStage(pipeline, "canaryJudge", "Perform Analysis", canaryJudgeContext);
    controlFetchContexts.forEach(
        (context) ->
            addStage(
                pipeline,
                (String) context.get("stageType"),
                (String) context.get("refId"),
                context));
    fetchExperimentContexts.forEach(
        (context) ->
            addStage(
                pipeline,
                (String) context.get("stageType"),
                (String) context.get("refId"),
                context));

    executionRepository.store(pipeline);

    try {
      executionLauncher.start(pipeline);
    } catch (Throwable t) {
      handleStartupFailure(pipeline, t);
    }

    return CanaryExecutionResponse.builder().canaryExecutionId(pipeline.getId()).build();
  }

  protected PipelineExecution.AuthenticationDetails getAuthenticationDetails() {
    return PipelineExecutionImpl.AuthenticationHelper.build()
        .orElse(new PipelineExecution.AuthenticationDetails());
  }

  protected Map<String, Object> configureCanaryJudgeContext(
      String resolvedStorageAccountName,
      CanaryExecutionRequest canaryExecutionRequest,
      HashMap<String, Object> setupCanaryContext,
      CanaryClassifierThresholdsConfig orchestratorScoreThresholds)
      throws JsonProcessingException {
    String canaryExecutionRequestJSON = objectMapper.writeValueAsString(canaryExecutionRequest);
    setupCanaryContext.put("canaryExecutionRequest", canaryExecutionRequestJSON);

    return new HashMap<>(
        Map.of(
            "refId",
            CanaryStageNames.REFID_JUDGE,
            "requisiteStageRefIds",
            Collections.singletonList(CanaryStageNames.REFID_MIX_METRICS),
            "user",
            ANONYMOUS_USER,
            "storageAccountName",
            resolvedStorageAccountName,
            "metricSetPairListId",
            "${ #stage('Mix Control and Experiment Results')['context']['metricSetPairListId']}",
            "orchestratorScoreThresholds",
            orchestratorScoreThresholds));
  }

  protected Map<String, Object> configureMixMetricSetsContext(
      CanaryConfig canaryConfig, String resolvedStorageAccountName) {
    int maxMetricIndex =
        canaryConfig.getMetrics().size()
            - 1; // 0 based naming, so we want the last index value, not the count
    String lastControlFetchRefid = CanaryStageNames.REFID_FETCH_CONTROL_PREFIX + maxMetricIndex;
    String lastExperimentFetchRefid =
        CanaryStageNames.REFID_FETCH_EXPERIMENT_PREFIX + maxMetricIndex;

    return new HashMap<>(
        Map.of(
            "refId",
            CanaryStageNames.REFID_MIX_METRICS,
            "requisiteStageRefIds",
            List.of(lastControlFetchRefid, lastExperimentFetchRefid),
            "user",
            ANONYMOUS_USER,
            "storageAccountName",
            resolvedStorageAccountName,
            "controlRefidPrefix",
            CanaryStageNames.REFID_FETCH_CONTROL_PREFIX,
            "experimentRefidPrefix",
            CanaryStageNames.REFID_FETCH_EXPERIMENT_PREFIX));
  }

  protected HashMap<String, Object> configureSetupCanaryContext(
      String application,
      String parentPipelineExecutionId,
      String canaryConfigId,
      CanaryConfig canaryConfig,
      String resolvedConfigurationAccountName,
      String resolvedMetricsAccountName,
      String resolvedStorageAccountName) {
    var mapBuilder =
        new ImmutableMap.Builder<String, Object>()
            .put("refId", CanaryStageNames.REFID_SET_CONTEXT)
            .put("user", ANONYMOUS_USER)
            .put("application", application)
            .put("storageAccountName", resolvedStorageAccountName)
            .put("canaryConfig", canaryConfig);

    if (resolvedMetricsAccountName != null) {
      mapBuilder.put("metricsAccountName", resolvedMetricsAccountName);
    }

    if (parentPipelineExecutionId != null) {
      mapBuilder.put("parentPipelineExecutionId", parentPipelineExecutionId);
    }

    HashMap<String, Object> setupCanaryContext = new HashMap(mapBuilder.build());
    if (resolvedConfigurationAccountName != null) {
      setupCanaryContext.put("configurationAccountName", resolvedConfigurationAccountName);
    }
    if (canaryConfigId != null) {
      setupCanaryContext.put("canaryConfigId", canaryConfigId);
    }
    return setupCanaryContext;
  }

  /**
   * Validates a Canary Executions Configuration prior to execution
   *
   * @param canaryConfig The canary configuration
   * @param canaryExecutionRequest The execution request
   */
  protected void validateCanaryExecutionConfiguration(
      CanaryConfig canaryConfig, CanaryExecutionRequest canaryExecutionRequest) {
    if (canaryConfig.getMetrics().size() <= 0) {
      throw new IllegalArgumentException(
          "The canary config must specify at least one metric. Otherwise we're not analyzing anything. :)");
    }
    Set<String> requiredScopes =
        canaryConfig.getMetrics().stream()
            .map(CanaryMetricConfig::getScopeName)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    if (requiredScopes.size() > 0 && canaryExecutionRequest.getScopes() == null) {
      throw new IllegalArgumentException(
          "Canary metrics require scopes, but no scopes were provided in the execution request.");
    }
    Set<String> providedScopes =
        canaryExecutionRequest.getScopes() == null
            ? Collections.emptySet()
            : canaryExecutionRequest.getScopes().keySet();
    requiredScopes.removeAll(providedScopes);
    if (requiredScopes.size() > 0) {
      throw new IllegalArgumentException(
          "Canary metrics require scopes which were not provided in the execution request: "
              + requiredScopes);
    }

    if (canaryExecutionRequest.getThresholds() == null) {
      throw new IllegalArgumentException("Execution request must contain thresholds");
    }
  }

  public CanaryExecutionResponse buildJudgeComparisonExecution(
      String application,
      String parentPipelineExecutionId,
      @NotNull String canaryConfigId,
      @NotNull CanaryConfig canaryConfig,
      String overrideCanaryJudge1,
      String overrideCanaryJudge2,
      String metricSetPairListId,
      Double passThreshold,
      Double marginalThreshold,
      String resolvedConfigurationAccountName,
      @NotNull String resolvedStorageAccountName) {

    if (StringUtils.isEmpty(application)) {
      application = "kayenta-" + currentInstanceId;
    }

    canaryConfig = QueryConfigUtils.escapeTemplates(canaryConfig);

    var setupCanaryContext =
        configureSetupCanaryContext(
            application,
            parentPipelineExecutionId,
            canaryConfigId,
            canaryConfig,
            resolvedConfigurationAccountName,
            null,
            resolvedStorageAccountName);

    var canaryJudgeContext1 =
        new HashMap<>(
            Map.of(
                "refId",
                CanaryStageNames.REFID_JUDGE,
                "requisiteStageRefIds",
                Collections.singletonList(CanaryStageNames.REFID_SET_CONTEXT),
                "user",
                ANONYMOUS_USER,
                "storageAccountName",
                resolvedStorageAccountName,
                "metricSetPairListId",
                metricSetPairListId,
                "orchestratorScoreThresholds",
                CanaryClassifierThresholdsConfig.builder()
                    .pass(passThreshold)
                    .marginal(marginalThreshold)
                    .build()));
    if (StringUtils.isNotEmpty(overrideCanaryJudge1)) {
      canaryJudgeContext1.put("overrideJudgeName", overrideCanaryJudge1);
    }

    var canaryJudgeContext2 =
        new HashMap<>(
            Map.of(
                "refId",
                CanaryStageNames.REFID_JUDGE + "-2",
                "requisiteStageRefIds",
                Collections.singletonList(CanaryStageNames.REFID_SET_CONTEXT),
                "user",
                ANONYMOUS_USER,
                "storageAccountName",
                resolvedStorageAccountName,
                "metricSetPairListId",
                metricSetPairListId,
                "orchestratorScoreThresholds",
                CanaryClassifierThresholdsConfig.builder()
                    .pass(passThreshold)
                    .marginal(marginalThreshold)
                    .build()));
    if (StringUtils.isNotEmpty(overrideCanaryJudge2)) {
      canaryJudgeContext2.put("overrideJudgeName", overrideCanaryJudge2);
    }

    var compareJudgeResultsContext =
        new HashMap<>(
            Map.of(
                "refId",
                "compareJudgeResults",
                "requisiteStageRefIds",
                Arrays.asList(CanaryStageNames.REFID_JUDGE, CanaryStageNames.REFID_JUDGE + "-2"),
                "user",
                ANONYMOUS_USER,
                "storageAccountName",
                resolvedStorageAccountName,
                "judge1Result",
                "${ #stage('Perform Analysis with Judge 1')['context']['result']}",
                "judge2Result",
                "${ #stage('Perform Analysis with Judge 2')['context']['result']}"));

    String canaryPipelineConfigId = application + "-standard-canary-pipeline";
    var pipeline = new PipelineExecutionImpl(PIPELINE, application);
    pipeline.setName("Standard Canary Pipeline");
    pipeline.setPipelineConfigId(canaryPipelineConfigId);
    pipeline.setLimitConcurrent(false);

    addStage(pipeline, "setupCanary", "Setup Canary", setupCanaryContext);
    addStage(pipeline, "canaryJudge", "Perform Analysis with Judge 1", canaryJudgeContext1);
    addStage(pipeline, "canaryJudge", "Perform Analysis with Judge 2", canaryJudgeContext2);
    addStage(pipeline, "compareJudgeResults", "Compare Judge Results", compareJudgeResultsContext);

    executionRepository.store(pipeline);

    try {
      executionLauncher.start(pipeline);
    } catch (Throwable t) {
      handleStartupFailure(pipeline, t);
    }

    return CanaryExecutionResponse.builder().canaryExecutionId(pipeline.getId()).build();
  }

  protected void handleStartupFailure(PipelineExecution execution, Throwable failure) {
    final String canceledBy = "system";
    final String reason = "Failed on startup: " + failure.getMessage();
    final ExecutionStatus status = ExecutionStatus.TERMINAL;

    log.error("Failed to start {} {}", execution.getType(), execution.getId(), failure);
    executionRepository.updateStatus(execution.getType(), execution.getId(), status);
    executionRepository.cancel(execution.getType(), execution.getId(), canceledBy, reason);

    registry.counter(failureId).increment();
  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  /**
   * Port of withStage from pipeline builder since its deprecated
   *
   * @param pipeline The pipeline to add the stage to
   * @param type The stage type
   * @param name The stage name
   * @param context The context for the stage
   */
  void addStage(
      PipelineExecutionImpl pipeline, String type, String name, Map<String, Object> context) {
    if (context.get("providerType") != null
        && !(Arrays.asList("aws", "titus")).contains(context.get("providerType"))) {
      type += "_" + context.get("providerType");
    }
    pipeline.getStages().add(new StageExecutionImpl(pipeline, type, name, context));
  }
}
