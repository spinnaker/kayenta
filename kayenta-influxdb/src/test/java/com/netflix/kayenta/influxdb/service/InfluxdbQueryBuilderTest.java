package com.netflix.kayenta.influxdb.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.netflix.kayenta.influxdb.canary.InfluxdbCanaryScope;
import com.netflix.kayenta.influxdb.metrics.InfluxdbQueryBuilder;

public class InfluxdbQueryBuilderTest {

  private InfluxdbQueryBuilder queryBuilder = new InfluxdbQueryBuilder();
  
  @Test
  public void testBuild_noScope() {
    String measurement = "temperature";
    
    InfluxdbCanaryScope canaryScope = createScope();
    String query = queryBuilder.build(measurement, fieldsList(), canaryScope);
    assertThat(query, is("SELECT external, internal FROM temperature WHERE  time >= '2010-01-01T12:00:00Z' AND  time < '2010-01-01T12:01:40Z'"));
  }

  private InfluxdbCanaryScope createScope() {
    InfluxdbCanaryScope canaryScope = new InfluxdbCanaryScope();
    canaryScope.setStart(Instant.ofEpochSecond(1262347200));
    canaryScope.setEnd(Instant.ofEpochSecond(1262347300));
    return canaryScope;
  }

  private List<String> fieldsList() {
    List<String> fields = new ArrayList<>();
    fields.add("external");
    fields.add("internal");
    return fields;
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testBuild_withInvalidScope() {
    String measurement = "temperature";
    
    InfluxdbCanaryScope canaryScope = createScope();
    canaryScope.setScope("server='myapp-prod-v002'");
    queryBuilder.build(measurement, fieldsList(), canaryScope);
  }
  
  @Test
  public void testBuild_withValidScope() {
    String measurement = "temperature";
    
    InfluxdbCanaryScope canaryScope = createScope();
    canaryScope.setScope("server:myapp-prod-v002");
    String query = queryBuilder.build(measurement, fieldsList(), canaryScope);
    assertThat(query, is("SELECT external, internal FROM temperature WHERE  time >= '2010-01-01T12:00:00Z' AND  time < '2010-01-01T12:01:40Z' AND server='myapp-prod-v002'"));
  }
  
  @Test
  public void testBuild_withNoFieldsSpecified() {
    String measurement = "temperature";
    
    InfluxdbCanaryScope canaryScope = createScope();
    canaryScope.setScope("server:myapp-prod-v002");
    String query = queryBuilder.build(measurement, null, canaryScope);
    assertThat(query, is("SELECT *::field FROM temperature WHERE  time >= '2010-01-01T12:00:00Z' AND  time < '2010-01-01T12:01:40Z' AND server='myapp-prod-v002'"));
  }

}
