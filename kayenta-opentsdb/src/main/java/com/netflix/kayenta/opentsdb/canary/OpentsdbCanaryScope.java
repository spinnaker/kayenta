package com.netflix.kayenta.opentsdb.canary;

import com.netflix.kayenta.canary.CanaryScope;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OpentsdbCanaryScope extends CanaryScope {
    @NotNull
    private String scopeKey;
}
