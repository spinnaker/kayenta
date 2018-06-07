/*
 * Copyright 2017 Google, Inc.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.kayenta.influxdb.model.InfluxdbResult;

import lombok.extern.slf4j.Slf4j;
import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

@Component
@Slf4j
public class InfluxdbResponseConverter implements Converter {

  private final ObjectMapper kayentaObjectMapper;

  @Autowired
  public InfluxdbResponseConverter(ObjectMapper kayentaObjectMapper) {
    this.kayentaObjectMapper = kayentaObjectMapper;
  }

  @Override
  public Object fromBody(TypedInput body, Type type) throws ConversionException {
    
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(body.in()))) {
      String json = reader.readLine();
      log.debug("Converting response from influxdb: {}", json);
      Map responseMap = kayentaObjectMapper.readValue(json, Map.class);
      List<Map> results = (List<Map>) responseMap.get("results");
      
      if (CollectionUtils.isEmpty(results)) {
        throw new ConversionException("Unexpected response from influxdb");
      }
      Map firstResult = (Map)results.get(0); //TODO(joerajeev): Check if there a need to support multi -queries (See https://docs.influxdata.com/influxdb/v1.5/guides/querying_data/#multiple-queries)
      List<Map> series = (List<Map>) firstResult.get("series");
      
      if (CollectionUtils.isEmpty(series)) {
        log.warn("Received no data from Influxdb.");
        return null;
      }
      
      Map firstSeries = series.get(0); //TODO(joerajeev): check if we can get multiple series elements
      List<String> seriesColumns = (List<String>) firstSeries.get("columns");
      List<List> seriesValues = (List<List>) firstSeries.get("values");
      List<InfluxdbResult> influxdbResultsList = new ArrayList<InfluxdbResult>(seriesValues.size());

      //TODO(joerajeev): if returning tags (other than the field names) we will need to skip tags from this loop,
      //and to extract and set the tag values to the influxdb result.
      for (int i=1; i<seriesColumns.size(); i++) {  //Start from index 1 to skip 'time' column
        
        String id = seriesColumns.get(i); 
        long firstTimeMillis = extractTimeInMillis(seriesValues, 0);
        long nextTimeMillis = extractTimeInMillis(seriesValues, 1);
        // If there aren't at least two data points, consider the step size to be zero.
        long stepMillis =
            seriesValues.size() > 1
          ? nextTimeMillis - firstTimeMillis
          : 0;
          
        List<Double> values = new ArrayList<>(seriesValues.size());
        for (List<Object> valueRow: seriesValues) {
          if (valueRow.get(i) != null) {
            values.add(Double.valueOf((Integer)valueRow.get(i)));
          } 
        }
        influxdbResultsList.add(new InfluxdbResult(id, firstTimeMillis, stepMillis, null, values));  //TODO: add support for tags
      }

      log.debug("Converted response: {} ", influxdbResultsList);
      return influxdbResultsList;
    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  private long extractTimeInMillis(List<List> seriesValues, int index) {
    String firstUtcTime = (String) seriesValues.get(index).get(0);  //TODO(joerajeev): check if I need to order it first 
    long startTimeMillis = Instant.parse(firstUtcTime).toEpochMilli();
    return startTimeMillis;
  }
  
  @Override
  public TypedOutput toBody(Object object) {
    return null;
  }
}
