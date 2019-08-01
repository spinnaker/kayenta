/*
 * Copyright 2018 Adobe
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

package com.netflix.kayenta.newrelic.canary;

import static com.netflix.kayenta.canary.providers.metrics.NewRelicCanaryMetricSetQueryConfig.SERVICE_TYPE;

import com.netflix.kayenta.canary.CanaryScope;
import com.netflix.kayenta.canary.CanaryScopeFactory;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class NewRelicCanaryScopeFactory implements CanaryScopeFactory {
  public static final String SCOPE_KEY_KEY = "_scope_key";
  public static String LOCATION_KEY_KEY = "_location_key";

  @Override
  public boolean handles(String serviceType) {
    return SERVICE_TYPE.equals(serviceType);
  }

  @Override
  public CanaryScope buildCanaryScope(CanaryScope canaryScope) {

    NewRelicCanaryScope newRelicCanaryScope = new NewRelicCanaryScope();
    newRelicCanaryScope.setScope(canaryScope.getScope());
    newRelicCanaryScope.setLocation(canaryScope.getLocation());
    newRelicCanaryScope.setStart(canaryScope.getStart());
    newRelicCanaryScope.setEnd(canaryScope.getEnd());
    newRelicCanaryScope.setStep(canaryScope.getStep());

    Optional.ofNullable(canaryScope.getExtendedScopeParams())
        .ifPresent(
            extendedParameters -> {
              newRelicCanaryScope.setScopeKey(extendedParameters.getOrDefault(SCOPE_KEY_KEY, null));
              newRelicCanaryScope.setLocationKey(
                  extendedParameters.getOrDefault(LOCATION_KEY_KEY, null));
              newRelicCanaryScope.setExtendedScopeParams(extendedParameters);
            });

    return newRelicCanaryScope;
  }
}
