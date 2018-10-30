package com.netflix.kayenta.graphite.canary;

import com.netflix.kayenta.canary.CanaryScope;
import com.netflix.kayenta.canary.CanaryScopeFactory;
import org.springframework.stereotype.Component;

@Component
public class GraphiteCanaryScopeFactory implements CanaryScopeFactory {
    @Override
    public boolean handles(String serviceType) {
        return "graphite".equalsIgnoreCase(serviceType);
    }

    @Override
    public CanaryScope buildCanaryScope(CanaryScope scope) {
        return scope;
    }
}
