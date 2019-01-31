package com.netflix.kayenta.opentsdb.metrics;

import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.canary.CanaryMetricConfig;
import com.netflix.kayenta.canary.CanaryScope;
import com.netflix.kayenta.canary.providers.metrics.OpentsdbCanaryMetricSetQueryConfig;
import com.netflix.kayenta.opentsdb.model.OpentsdbResults;
import com.netflix.kayenta.opentsdb.security.OpentsdbCredentials;
import com.netflix.kayenta.opentsdb.security.OpentsdbNamedAccountCredentials;
import com.netflix.kayenta.opentsdb.service.OpentsdbRemoteService;
import com.netflix.kayenta.metrics.MetricSet;
import com.netflix.kayenta.metrics.MetricsService;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.kayenta.security.CredentialsHelper;
import com.netflix.spectator.api.Registry;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import com.netflix.kayenta.opentsdb.canary.OpentsdbCanaryScope;
import com.netflix.kayenta.opentsdb.model.OpentsdbMetricDescriptorsResponse;
import com.netflix.kayenta.opentsdb.model.OpentsdbMetricDescriptor;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Builder
@Slf4j
public class OpentsdbMetricsService implements MetricsService {
  private static final String DELIMITER = ".";

  @NotNull
  @Singular
  @Getter
  private List<String> accountNames;

  @Autowired
  private final AccountCredentialsRepository accountCredentialsRepository;

  @Autowired
  private final Registry registry;

  @Override
  public String getType() {
    return "opentsdb";
  }

  @Override
  public boolean servicesAccount(String accountName) {
    return accountNames.contains(accountName);
  }

  @Override
  public String buildQuery(String metricsAccountName, CanaryConfig canaryConfig, CanaryMetricConfig canaryMetricConfig,
                           CanaryScope canaryScope)  {

    OpentsdbCanaryScope opentsdbCanaryScope = (OpentsdbCanaryScope)canaryScope;
    OpentsdbCanaryMetricSetQueryConfig queryConfig =
            (OpentsdbCanaryMetricSetQueryConfig) canaryMetricConfig.getQuery();

    String aggregator = Optional.ofNullable(queryConfig.getAggregator()).orElse("sum");
    String downsample = Optional.ofNullable(queryConfig.getDownsample()).orElse("");
    List<TagPair> tagPairs = Optional.ofNullable(queryConfig.getTags()).orElse(new LinkedList<>());

    return OpentsdbQueryBuilder
            .create(queryConfig.getMetricName(), aggregator, downsample, queryConfig.isRate())
            .withTagPairs(tagPairs)
            .withScope(opentsdbCanaryScope)
            .build();
  }

  @Override
  public List<MetricSet> queryMetrics(String accountName, CanaryConfig canaryConfig, CanaryMetricConfig canaryMetricConfig, CanaryScope canaryScope) {
    if (!(canaryScope instanceof OpentsdbCanaryScope)) {
      throw new IllegalArgumentException("Canary scope not instance of OpentsdbCanaryScope: " + canaryScope +
              ". One common cause is having multiple METRICS_STORE accounts configured but " +
              "neglecting to explicitly specify which account to use for a given request.");
    }

    OpentsdbNamedAccountCredentials accountCredentials =
            (OpentsdbNamedAccountCredentials) accountCredentialsRepository.getOne(accountName)
                    .orElseThrow(() -> new IllegalArgumentException("Unable to resolve account " + accountName + "."));

    String query = buildQuery(accountName,
            canaryConfig,
            canaryMetricConfig,
            canaryScope);

    log.info("query = {}", query);

    OpentsdbRemoteService remoteService = accountCredentials.getOpentsdbRemoteService();

    OpentsdbResults opentsdbResults = remoteService.fetch(
            query,
            canaryScope.getStart().getEpochSecond(),
            canaryScope.getEnd().getEpochSecond()
    );
    log.info("canaryScope={}", canaryScope);
    log.info("adjustedPoints={}", opentsdbResults.getAdjustedDataValues(canaryScope.getStep(),
            canaryScope.getStart().getEpochSecond(),
            canaryScope.getEnd().getEpochSecond()));
    List<MetricSet> ret = new ArrayList<MetricSet>();
    ret.add(MetricSet.builder().name(canaryMetricConfig.getName())
                               .startTimeMillis(canaryScope.getStart().getEpochSecond() * 1000)
                               .startTimeIso(Instant.ofEpochSecond(canaryScope.getStart().getEpochSecond() * 1000).toString())
                               .endTimeMillis(canaryScope.getEnd().getEpochSecond() * 1000)
                               .endTimeIso(Instant.ofEpochSecond(canaryScope.getEnd().getEpochSecond() * 1000).toString())
                               .stepMillis(canaryScope.getStep() * 1000)
                               .values(opentsdbResults.getAdjustedDataValues(canaryScope.getStep(),
                                       canaryScope.getStart().getEpochSecond(),
                                       canaryScope.getEnd().getEpochSecond()))
                               .attribute("query", query)
                               .build());

    return ret;
  }

    //TODO: in case of performance issue when there are lots of users, we could cache last responses
    @Override
    public List<Map> getMetadata(String metricsAccountName, String filter) throws IOException {
        log.info(String.format("Getting metadata for %s with filter %s", metricsAccountName, filter));

        List<Map> result = new LinkedList<>();

            OpentsdbNamedAccountCredentials accountCredentials =
                    (OpentsdbNamedAccountCredentials) accountCredentialsRepository.getOne(metricsAccountName)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    String.format("Unable to resolve account %s.", metricsAccountName)));

            OpentsdbRemoteService remoteService = accountCredentials.getOpentsdbRemoteService();

            OpentsdbMetricDescriptorsResponse opentsdbMetricDescriptorsResponse = remoteService.findMetrics(filter);

            log.debug(String.format("Getting response for %s with response size %d",
                    metricsAccountName, opentsdbMetricDescriptorsResponse.getMetrics().size()));

             opentsdbMetricDescriptorsResponse.getMetrics().stream().forEach(
                     name -> result.add(new OpentsdbMetricDescriptor(name).getMap()));

        return result;
    }  
}
