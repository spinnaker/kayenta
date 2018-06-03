package com.netflix.kayenta.influxdb.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.kayenta.influxdb.model.InfluxdbResult;

import retrofit.converter.ConversionException;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

public class InfluxdbResponseConverterTest {

  private static final String MIME_TYPE = "application/json; charset=UTF-8";
  private final String JSON = "{\"results\":[{\"statement_id\":0,\"series\":[{\"name\":\"temperature\",\"columns\":[\"time\",\"external\",\"internal\"],\"values\":[[\"2018-05-27T04:50:44.105612486Z\",25,37],[\"2018-05-27T04:51:44.105612486Z\",25,37],[\"2018-05-27T04:52:06.585796188Z\",26,38]]}]}]}";
  private List<InfluxdbResult> results = new ArrayList<>();
  
  @Before
  public void setup() {
  /*  Map<String, String> tags = new HashMap<>();
    tags.put("host", "serverA");
    tags.put("reigion", "melbourne");*/
    List<Double> externalDataValues = new ArrayList<>();
    externalDataValues.add(25d);
    externalDataValues.add(25d);
    externalDataValues.add(26d);
    //InfluxdbResult influxdbResult = new InfluxdbResult("temperature-external", 1435781430781L, 1, 1435781430881L, null, values);
    InfluxdbResult externalTempResult = new InfluxdbResult("external", 1527396644105L, 60000L, null, externalDataValues);
    results.add(externalTempResult);
    
    List<Double> internalDataValues = new ArrayList<>();
    internalDataValues.add(37d);
    internalDataValues.add(37d);
    internalDataValues.add(38d);
    //InfluxdbResult influxdbResult = new InfluxdbResult("temperature-external", 1435781430781L, 1, 1435781430881L, null, values);
    InfluxdbResult internalTempResult = new InfluxdbResult("internal", 1527396644105L, 60000L, null, internalDataValues);
    results.add(internalTempResult);
  }

  private final InfluxdbResponseConverter influxdbResponseConverter = new InfluxdbResponseConverter(new ObjectMapper());

  @Test public void serialize() throws Exception {
    assertThat(influxdbResponseConverter.toBody(results), is(nullValue()));
  }

  @Test 
  public void deserialize() throws Exception {
    TypedInput input = new TypedByteArray(MIME_TYPE, JSON.getBytes());
    List<InfluxdbResult> result = (List<InfluxdbResult>) influxdbResponseConverter.fromBody(input, List.class);
    assertThat(result, is(results));
  }

  @Test(expected = ConversionException.class)
  public void deserializeWrongValue() throws Exception {
    TypedInput input = new TypedByteArray(MIME_TYPE, "{\"foo\":\"bar\"}".getBytes());
    influxdbResponseConverter.fromBody(input, List.class);
  }

  private String asString(TypedOutput typedOutput) throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    typedOutput.writeTo(bytes);
    return new String(bytes.toByteArray());
  }

}
