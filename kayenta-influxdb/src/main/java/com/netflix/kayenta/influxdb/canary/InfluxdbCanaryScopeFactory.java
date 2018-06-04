/*
 * Copyright 2018 Armory, Inc.
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

package com.netflix.kayenta.influxdb.canary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.netflix.kayenta.canary.CanaryScope;
import com.netflix.kayenta.canary.CanaryScopeFactory;

@Component
public class InfluxdbCanaryScopeFactory implements CanaryScopeFactory {
  @Override
  public boolean handles(String serviceType) {
    return serviceType.equals("influxdb");
  }

  @Override
  public CanaryScope buildCanaryScope(CanaryScope canaryScope) {
    InfluxdbCanaryScope influxdbCanaryScope = new InfluxdbCanaryScope();
    influxdbCanaryScope.setScope(canaryScope.getScope());
    influxdbCanaryScope.setLocation(canaryScope.getLocation());
    influxdbCanaryScope.setStart(canaryScope.getStart());
    influxdbCanaryScope.setEnd(canaryScope.getEnd());
    influxdbCanaryScope.setStep(canaryScope.getStep());
    influxdbCanaryScope.setExtendedScopeParams(canaryScope.getExtendedScopeParams());

    Map<String, String> extendedScopeParams = influxdbCanaryScope.getExtendedScopeParams();

    if (extendedScopeParams != null) {
      if (extendedScopeParams.containsKey("fields")) {
        String fieldsStr = extendedScopeParams.get("fields");
        List<String> fields = new ArrayList<>();
        if (fieldsStr.contains(",")) {
          fields = Arrays.asList(fieldsStr.split(","));
        } else {
          fields.add(fieldsStr);
        }
        influxdbCanaryScope.setFields(fields);
      }
    }

    return influxdbCanaryScope;
  }
}
