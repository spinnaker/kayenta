# Configuring Kayenta

<!-- This doc can be way better, stub it out for now and will at least link the the big kayenta.yml that has examples in it -->

## At least one Configured Object Store

Kayenta uses the object store to save its results, so this is Kayenta's data store more or less.

## At least one Configured Metrics Store

Metric stores are the the actual integration of a metric source (Prometheus, Stackdriver, Atlas, SignalFx, etc) into Kayenta.

## At least one Configured Configuration Store

Configuration stores are where Kayenta will save canary configuration it is told to save, its often fine to have the Object and Config store be the same thing.

## Examples

See the reference [kayenta.yml](../kayenta-web/config/kayenta.yml) for the available config options.
See the [SignalFx Integration Test Config](../kayenta-signalfx/src/integration-test/resources/config/kayenta.yml) for a working example
