package com.netflix.kayenta.graphite.cancary;

import com.netflix.kayenta.canary.CanaryScope;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class GraphiteCanaryScope extends CanaryScope {
}
