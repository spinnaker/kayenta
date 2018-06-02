package com.netflix.kayenta.influxdb.metrics;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.netflix.kayenta.canary.CanaryScope;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class InfluxdbQueryBuilder {
  
  //TODO(joerajeev): update to accept tags and groupby fields (and steps?)
  //TODO(joerajeev): protect against sql injection
  public String build(String measurement, List<String> fields, CanaryScope canaryScope) {
    //Validations
    if (CollectionUtils.isEmpty(fields)) {
      throw new IllegalArgumentException("At least one field required to query metrics");
    }
    if (StringUtils.isEmpty(measurement)) {
      throw new IllegalArgumentException("Measurement is required to query metrics");
    }
    
    StringBuilder sb = new StringBuilder();
    sb.append("SELECT ");
    sb.append(fields.stream().collect(Collectors.joining(", ")));
    sb.append(" FROM ");
    sb.append(measurement);
    sb.append(" WHERE ");
    sb.append(" time >= '"+ canaryScope.getStart().toString() + "'");
    sb.append(" AND ");
    sb.append(" time < '"+ canaryScope.getEnd().toString() + "'");
    
    String scope = canaryScope.getScope();
    if (scope != null && scope.contains(":")) {
      sb.append( " AND ");
      String[] scopeParts = scope.split(":");
      if (scopeParts.length != 2) {
        throw new IllegalArgumentException("Scope expected in the format of 'name:value'. e.g. autoscaling_group:myapp-prod-v002");
      }
      sb.append(scopeParts[0] + "='" + scopeParts[1] +"'");
    }
    
    log.info("Built query :{}", sb.toString());
    
    return sb.toString();
  }  

}
