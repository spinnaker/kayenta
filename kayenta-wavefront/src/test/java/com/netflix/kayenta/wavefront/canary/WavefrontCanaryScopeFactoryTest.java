package com.netflix.kayenta.wavefront.canary;


import com.netflix.kayenta.canary.CanaryScope;
import org.junit.Test;

import java.time.Instant;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class WavefrontCanaryScopeFactoryTest {

    private WavefrontCanaryScopeFactory queryBuilder = new WavefrontCanaryScopeFactory();

    @Test
    public void testBuildCanaryScope_WithSecondGranularity() {
        CanaryScope canaryScope = new CanaryScope("scope", "location", Instant.now(), Instant.now(), WavefrontCanaryScopeFactory.SECOND, null);
        CanaryScope generatedCanaryScope = queryBuilder.buildCanaryScope(canaryScope);
        WavefrontCanaryScope wavefrontCanaryScope = (WavefrontCanaryScope) generatedCanaryScope;
        assertThat(wavefrontCanaryScope.getGranularity(), is("s"));
    }

    @Test
    public void testBuildCanaryScope_WithMinuteGranularity() {
        CanaryScope canaryScope = new CanaryScope("scope", "location", Instant.now(), Instant.now(), WavefrontCanaryScopeFactory.MINUTE, null);
        CanaryScope generatedCanaryScope = queryBuilder.buildCanaryScope(canaryScope);
        WavefrontCanaryScope wavefrontCanaryScope = (WavefrontCanaryScope) generatedCanaryScope;
        assertThat(wavefrontCanaryScope.getGranularity(), is("m"));
    }

    @Test
    public void testBuildCanaryScope_WithHourGranularity() {
        CanaryScope canaryScope = new CanaryScope("scope", "location", Instant.now(), Instant.now(), WavefrontCanaryScopeFactory.HOUR, null);
        CanaryScope generatedCanaryScope = queryBuilder.buildCanaryScope(canaryScope);
        WavefrontCanaryScope wavefrontCanaryScope = (WavefrontCanaryScope) generatedCanaryScope;
        assertThat(wavefrontCanaryScope.getGranularity(), is("h"));
    }

    @Test
    public void testBuildCanaryScope_WithDayGranularity() {
        CanaryScope canaryScope = new CanaryScope("scope", "location", Instant.now(), Instant.now(), WavefrontCanaryScopeFactory.DAY, null);
        CanaryScope generatedCanaryScope = queryBuilder.buildCanaryScope(canaryScope);
        WavefrontCanaryScope wavefrontCanaryScope = (WavefrontCanaryScope) generatedCanaryScope;
        assertThat(wavefrontCanaryScope.getGranularity(), is("d"));
    }
}
