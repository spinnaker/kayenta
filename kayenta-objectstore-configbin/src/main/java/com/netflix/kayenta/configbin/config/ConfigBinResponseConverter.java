/*
 * Copyright 2017 Netflix, Inc.
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

package com.netflix.kayenta.configbin.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.RequestBody;
import lombok.extern.slf4j.Slf4j;
import okio.Buffer;
import org.springframework.stereotype.Component;
import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@Component
@Slf4j
public class ConfigBinResponseConverter implements Converter {

  private static final ObjectMapper objectMapper = new ObjectMapper()
    .setSerializationInclusion(NON_NULL)
    .disable(FAIL_ON_UNKNOWN_PROPERTIES);

  @Override
  public String fromBody(TypedInput body, Type type) throws ConversionException {
    try {
      JsonNode obj = objectMapper.readTree(body.in());
      return obj.get("payload").asText();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  @Override
  public TypedOutput toBody(Object object) {
    RequestBody requestBody = (RequestBody)object;
    return new StringTypedOutput(requestBody);
  }

  private static class StringTypedOutput implements TypedOutput {
    private final RequestBody string;

    StringTypedOutput(RequestBody s) { this.string = s; }

    @Override public String fileName() { return null; }

    @Override public String mimeType() { return "application/json; charset=UTF-8"; }

    @Override public long length() {
      try {
        return string.contentLength();
      } catch (IOException e) {
        return 0;
      }
    }

    @Override public void writeTo(OutputStream out) throws IOException {
      Buffer buffer = new Buffer();
      string.writeTo(buffer);
      buffer.writeTo(out);
    }
  }
}
