package com.netflix.kayenta.opentsdb.canary;

import com.netflix.kayenta.canary.CanaryScope;
import com.netflix.kayenta.canary.CanaryScopeFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

import static com.netflix.kayenta.canary.providers.metrics.OpentsdbCanaryMetricSetQueryConfig.SERVICE_TYPE;

@Component
public class OpentsdbCanaryScopeFactory implements CanaryScopeFactory {

  public static String SCOPE_KEY_KEY = "_scope_key";

  @Override
  public boolean handles(String serviceType) { return SERVICE_TYPE.equals(serviceType);
  }

  @Override
  public CanaryScope buildCanaryScope(CanaryScope canaryScope) {

    Map<String, String> extendedParameters = Optional.ofNullable(canaryScope.getExtendedScopeParams())
               .orElseThrow(() -> new IllegalArgumentException("Opentsdb requires extended parameters"));

    OpentsdbCanaryScope opentsdbCanaryScope = new OpentsdbCanaryScope();
    opentsdbCanaryScope.setScope(canaryScope.getScope());
    opentsdbCanaryScope.setLocation(canaryScope.getLocation());
    opentsdbCanaryScope.setStart(canaryScope.getStart());
    opentsdbCanaryScope.setEnd(canaryScope.getEnd());
    opentsdbCanaryScope.setStep(canaryScope.getStep());
    opentsdbCanaryScope.setScopeKey(getRequiredExtendedParam(SCOPE_KEY_KEY, extendedParameters));
    opentsdbCanaryScope.setExtendedScopeParams(extendedParameters);

    return opentsdbCanaryScope;
  }

  private String getRequiredExtendedParam(String key, Map<String, String> extendedParameters) {
    if (! extendedParameters.containsKey(key)) {
      throw new IllegalArgumentException(String.format("Opentsdb requires that %s is set in the extended scope params", key));
    }
    return extendedParameters.get(key);
  }
}
