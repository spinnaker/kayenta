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
import com.netflix.kayenta.influxdb.model.InfluxDbResult;

import retrofit.converter.ConversionException;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

public class InfluxDbResponseConverterTest {

  private static final String MIME_TYPE = "application/json; charset=UTF-8";
  private final String JSON = "{\"results\":[{\"statement_id\":0,\"series\":[{\"name\":\"temperature\",\"columns\":[\"time\",\"external\",\"internal\"],\"values\":[[\"2018-05-27T04:50:44.105612486Z\",25,37],[\"2018-05-27T04:51:44.105612486Z\",25,37],[\"2018-05-27T04:52:06.585796188Z\",26,38]]}]}]}";
  private List<InfluxDbResult> results = new ArrayList<>();
  
  @Before
  public void setup() {
    List<Double> externalDataValues = new ArrayList<>();
    externalDataValues.add(25d);
    externalDataValues.add(25d);
    externalDataValues.add(26d);
    InfluxDbResult externalTempResult = new InfluxDbResult("external", 1527396644105L, 60000L, null, externalDataValues);
    results.add(externalTempResult);
    
    List<Double> internalDataValues = new ArrayList<>();
    internalDataValues.add(37d);
    internalDataValues.add(37d);
    internalDataValues.add(38d);
    InfluxDbResult internalTempResult = new InfluxDbResult("internal", 1527396644105L, 60000L, null, internalDataValues);
    results.add(internalTempResult);
  }

  private final InfluxDbResponseConverter influxDbResponseConverter = new InfluxDbResponseConverter(new ObjectMapper());

  @Test public void serialize() throws Exception {
    assertThat(influxDbResponseConverter.toBody(results), is(nullValue()));
  }

  @Test 
  public void deserialize() throws Exception {
    TypedInput input = new TypedByteArray(MIME_TYPE, JSON.getBytes());
    List<InfluxDbResult> result = (List<InfluxDbResult>) influxDbResponseConverter.fromBody(input, List.class);
    assertThat(result, is(results));
  }

  @Test(expected = ConversionException.class)
  public void deserializeWrongValue() throws Exception {
    TypedInput input = new TypedByteArray(MIME_TYPE, "{\"foo\":\"bar\"}".getBytes());
    influxDbResponseConverter.fromBody(input, List.class);
  }

  private String asString(TypedOutput typedOutput) throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    typedOutput.writeTo(bytes);
    return new String(bytes.toByteArray());
  }

}
