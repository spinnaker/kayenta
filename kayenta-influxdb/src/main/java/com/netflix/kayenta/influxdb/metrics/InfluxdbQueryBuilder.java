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
    sb.append(" time >= "+ canaryScope.getStart().toString());
    sb.append(" AND ");
    sb.append(" time < "+ canaryScope.getEnd().toString());
    
    if (canaryScope.getScope() != null) {
      sb.append( " AND ");
      sb.append(canaryScope.getScope());
    }
    
    log.debug("Built query :{}", sb.toString());
    
    return sb.toString();
  }  

}
