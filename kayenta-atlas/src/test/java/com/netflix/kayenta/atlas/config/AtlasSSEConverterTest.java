package com.netflix.kayenta.atlas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.kayenta.atlas.model.AtlasResults;
import com.netflix.kayenta.metrics.RetryableQueryException;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.*;

public class AtlasSSEConverterTest {

  private String closeMessage = "data: { \"type\": \"close\" }\n";
  private String timeseriesMessage = "data: {\"type\":\"timeseries\",\"id\":\"randomIdHere\",\"query\":\"name,apache.http.request,:eq,statistic,count,:eq,:and,:sum,(,status,method,),:by\",\"tags\":{\"method\":\"get\",\"name\":\"apache.http.request\",\"statistic\":\"count\",\"atlas.offset\":\"0w\",\"status\":\"2xx\",\"nf.cluster\":\"foocluster\"},\"start\":1517860320000,\"end\":1517863920000,\"step\":60000,\"data\":{\"type\":\"array\",\"values\":[0.8666666666666667]}}\n";
  private String errorMessageIllegalStateMessage = "data: {\"type\":\"error\",\"message\":\"IllegalStateException: unknown word ':eqx'\"}\n";
  private String retryableErrorMessage = "data: {\"type\":\"error\",\"message\":\"something went wrong\"}\n";

  @Test
  public void loneClose() {
    AtlasSSEConverter atlasSSEConverter = new AtlasSSEConverter(new ObjectMapper());

    String sse = closeMessage;

    BufferedReader reader = new BufferedReader(new StringReader(sse));

    List<AtlasResults> results = atlasSSEConverter.processInput(reader);

    assertEquals(1, results.size());
  }

  @Test
  public void dataPlusClose() {
    AtlasSSEConverter atlasSSEConverter = new AtlasSSEConverter(new ObjectMapper());

    String sse = timeseriesMessage + closeMessage;

    BufferedReader reader = new BufferedReader(new StringReader(sse));

    List<AtlasResults> results = atlasSSEConverter.processInput(reader);

    assertEquals(2, results.size());
  }

  @Test(expected = IllegalStateException.class)
  public void missingCloseThrows() {
    AtlasSSEConverter atlasSSEConverter = new AtlasSSEConverter(new ObjectMapper());

    String sse = timeseriesMessage;

    BufferedReader reader = new BufferedReader(new StringReader(sse));

    List<AtlasResults> results = atlasSSEConverter.processInput(reader);
  }

  @Test(expected = IllegalStateException.class)
  public void containsRetryableErrorWithoutClose() {
    AtlasSSEConverter atlasSSEConverter = new AtlasSSEConverter(new ObjectMapper());

    String sse = errorMessageIllegalStateMessage;

    BufferedReader reader = new BufferedReader(new StringReader(sse));

    List<AtlasResults> results = atlasSSEConverter.processInput(reader);
  }

  @Test(expected = RetryableQueryException.class)
  public void containsRetryableErrorWithClose() {
    AtlasSSEConverter atlasSSEConverter = new AtlasSSEConverter(new ObjectMapper());

    String sse = retryableErrorMessage;

    BufferedReader reader = new BufferedReader(new StringReader(sse));

    List<AtlasResults> results = atlasSSEConverter.processInput(reader);
  }
}
