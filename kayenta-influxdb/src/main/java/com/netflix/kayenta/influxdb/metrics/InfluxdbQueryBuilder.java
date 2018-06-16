/*
 * Copyright 2018 Joseph Motha
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

package com.netflix.kayenta.influxdb.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.netflix.kayenta.canary.CanaryScope;
import com.netflix.kayenta.canary.providers.InfluxdbCanaryMetricSetQueryConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class InfluxdbQueryBuilder {
  
  //TODO(joerajeev): update to accept tags and groupby fields
  //TODO(joerajeev): protect against injection
  public String build(InfluxdbCanaryMetricSetQueryConfig queryConfig, CanaryScope canaryScope) {
    
    List<String> fields = queryConfig.getFields();
    String metricName = queryConfig.getMetricName();
    //Validations
    if (CollectionUtils.isEmpty(fields)) {
      if(fields == null) {
        fields = new ArrayList<>();
      }
      fields.add("*::field");
    }
    if (StringUtils.isEmpty(metricName)) {
      throw new IllegalArgumentException("Measurement is required to query metrics");
    }
    
    StringBuilder sb = new StringBuilder();
    buildBaseQuery(metricName, fields, sb);
    addTimeRangeFilter(canaryScope, sb);
    addScopeFilter(canaryScope, sb);
    
    log.debug("Built query :{}", sb.toString());
    
    return sb.toString();
  }

  private void buildBaseQuery(String measurement, List<String> fields, StringBuilder sb) {
    sb.append("SELECT ");
    sb.append(fields.stream().collect(Collectors.joining(", ")));
    sb.append(" FROM ");
    sb.append(measurement);
    sb.append(" WHERE ");
  }

  private void addScopeFilter(CanaryScope canaryScope, StringBuilder sb) {
    String scope = canaryScope.getScope();
    if (scope != null) {
      if (!scope.contains(":")) {
        throw new IllegalArgumentException("Scope expected in the format of 'name:value'. e.g. autoscaling_group:myapp-prod-v002");
      }
      sb.append( " AND ");
      String[] scopeParts = scope.split(":");
      if (scopeParts.length != 2) {
        throw new IllegalArgumentException("Scope expected in the format of 'name:value'. e.g. autoscaling_group:myapp-prod-v002");
      }
      sb.append(scopeParts[0] + "='" + scopeParts[1] +"'");
    }
  }

  private void addTimeRangeFilter(CanaryScope canaryScope, StringBuilder sb) {
    sb.append(" time >= '"+ canaryScope.getStart().toString() + "'");
    sb.append(" AND ");
    sb.append(" time < '"+ canaryScope.getEnd().toString() + "'");
  }  

}
