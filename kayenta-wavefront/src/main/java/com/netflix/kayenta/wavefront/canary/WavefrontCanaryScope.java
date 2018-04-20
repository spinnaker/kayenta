package com.netflix.kayenta.wavefront.canary;

import com.netflix.kayenta.canary.CanaryScope;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WavefrontCanaryScope extends CanaryScope {

    @NotNull
    private String granularity;

    public void setStepFromGranularity(String granularity) {
        if (granularity.equals("s")) {
            this.step = WavefrontCanaryScopeFactory.SECOND;
        }
        if (granularity.equals("m")) {
            this.step = WavefrontCanaryScopeFactory.MINUTE;
        }
        if (granularity.equals("h")) {
            this.step = WavefrontCanaryScopeFactory.HOUR;;
        }
        if (granularity.equals("d")) {
            this.step = WavefrontCanaryScopeFactory.DAY;;

        }
    }

}
