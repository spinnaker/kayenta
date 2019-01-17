package com.netflix.kayenta.opentsdb.metrics;

import com.netflix.kayenta.opentsdb.metrics.TagPair;
import com.netflix.kayenta.opentsdb.canary.OpentsdbCanaryScope;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Build an OpenTSDB formatted query string.
 */
public class OpentsdbQueryBuilder {

  public static final String FILTER_TEMPLATE = "%s=%s";
  private final String metricName;
  private final String aggregator;
  private final String downsample;
  private final boolean rate;

  private List<TagPair> tagPairs;
  private List<String> filterSegments;
  private List<String> scopeKeys;

  private OpentsdbQueryBuilder(String metricName, String aggregator, String downsample, boolean rate) {
    this.metricName = metricName;
    this.aggregator = aggregator;
    this.downsample = downsample;
    this.rate = rate;
    tagPairs = new LinkedList<>();
    filterSegments = new LinkedList<>();
    scopeKeys = new LinkedList<>();
  }

  public static OpentsdbQueryBuilder create(String metricName,
                                            String aggregator, String downsample, boolean rate) {

    return new OpentsdbQueryBuilder(metricName, aggregator, downsample, rate);
  }

  public OpentsdbQueryBuilder withTagPair(TagPair tagPair) {
    tagPairs.add(tagPair);
    return this;
  }

  public OpentsdbQueryBuilder withTagPairs(Collection<TagPair> tagPairs) {
    this.tagPairs.addAll(tagPairs);
    return this;
  }

  public OpentsdbQueryBuilder withScope(OpentsdbCanaryScope canaryScope) {
    scopeKeys.addAll(canaryScope.getExtendedScopeParams().keySet().stream()
            .filter(key -> !key.startsWith("_")).collect(Collectors.toList()));
    scopeKeys.add(canaryScope.getScopeKey());
    filterSegments.add(buildFilterSegmentFromScope(canaryScope));
    return this;
  }

  private String buildFilterSegmentFromScope(OpentsdbCanaryScope canaryScope) {

    List<String> filters = new LinkedList<>();

    filters.add(String.format(FILTER_TEMPLATE, canaryScope.getScopeKey(), canaryScope.getScope()));

    if (canaryScope.getExtendedScopeParams().size() > 0) {
      filters.addAll(canaryScope.getExtendedScopeParams().entrySet().stream()
              .filter(entry -> !entry.getKey().startsWith("_")) // filter out keys that start with _
              .map(entry -> String.format(FILTER_TEMPLATE, entry.getKey(), entry.getValue()))
              .collect(Collectors.toList()));
    }

    return String.join(",", filters);
  }

  public String build() {

    StringBuilder program = new StringBuilder(aggregator).append(":");

    if (!downsample.isEmpty()) {
      program.append(downsample).append(":");
    }

    if (rate) {
      program.append("rate:");
    }

    program.append(metricName).append("{");
    List<String> filters = new LinkedList<>();

    if (tagPairs.size() > 0) {
      filters.add(tagPairs.stream()
              .map(tp -> String.format(FILTER_TEMPLATE, tp.getKey(), tp.getValue()))
              .collect(Collectors.joining(",")));
    }
    filters.addAll(filterSegments);

    if (!filters.isEmpty()) {
      program.append(String.join(",", filters));
    }

    program.append("}");

    return program.toString();
  }
}
