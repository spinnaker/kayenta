package com.netflix.kayenta.opentsdb.config;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.kayenta.opentsdb.model.OpentsdbMetricDescriptorsResponse;
import com.netflix.kayenta.opentsdb.model.OpentsdbResults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.converter.JacksonConverter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class OpentsdbResponseConverter implements Converter {

  private static final int DEFAULT_STEP_SIZE = 0;
  private final ObjectMapper kayentaObjectMapper;

  @Autowired
  public OpentsdbResponseConverter(ObjectMapper kayentaObjectMapper) {
    this.kayentaObjectMapper = kayentaObjectMapper;
  }

  @Override
  public Object fromBody(TypedInput body, Type type) throws ConversionException {
    // Metric suggest
    //if (type == OpentsdbMetricDescriptorsResponse.class) {
     // return new JacksonConverter(kayentaObjectMapper).fromBody(body, type);
    //}
    // Metric query
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(body.in()))) {
      String json = reader.readLine();
      log.info("Converting response from opentsdb: {}", json);

      if (type != OpentsdbResults.class) {
        return this.getMetadataResultObject(json);
      }

      Map result = getResultObject(json);

      List<List> seriesValues = (List<List>) result.get("dps");

      if (CollectionUtils.isEmpty(seriesValues)) {
        log.warn("Received no data from Opentsdb.");
        return null;
      }

      OpentsdbResults opentsdbResults = new OpentsdbResults((String) result.get("metric"),
              (Map<String, String>) result.get("tags"), (List<List>) result.get("dps"));

      log.info("Converted response: {} ", opentsdbResults);

      return opentsdbResults;
    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  private List<String> getMetadataResultObject(String json)
          throws IOException, JsonParseException, JsonMappingException, ConversionException {
    return kayentaObjectMapper.readValue(json, new TypeReference<List<String>>() {});
  }

  private Map getResultObject(String json)
          throws IOException, JsonParseException, JsonMappingException, ConversionException {
    List<Map> results = kayentaObjectMapper.readValue(json, new TypeReference<List<Map>>() {});
    if (CollectionUtils.isEmpty(results)) {
      throw new ConversionException("Unexpected response from opentsdb");
    }
    if (results.size() > 1) {
      throw new ConversionException("Received too many series to deserialize");
    }

    return results.get(0);
  }

  @Override
  public TypedOutput toBody(Object object) {
    return null;
  }
}
