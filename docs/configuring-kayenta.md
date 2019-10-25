# Configuring Kayenta

The best way to configure Kayenta if your not using [Halyard](https://github.com/spinnaker/halyard) is to copy [kayenta.yml](../kayenta-web/config/kayenta.yml) and edit it.
Please note that you need a minimum of the following.

## At least one Configured Object Store

Kayenta uses the object store to save its results, so this is Kayenta's data store more or less.

## At least one Configured Metrics Store

Metric stores are the actual integration of a metric source (Prometheus, Stackdriver, Atlas, SignalFx, etc) into Kayenta.

## At least one Configured Configuration Store

Configuration stores are where Kayenta will save canary configuration it is told to save, its often fine to have the Object and Config store be the same thing.

## Examples

- See the reference [kayenta.yml](../kayenta-web/config/kayenta.yml) for the available config options.
- See the [SignalFx Integration Test Config](../kayenta-signalfx/src/integration-test/resources/config/kayenta.yml) for a working example
