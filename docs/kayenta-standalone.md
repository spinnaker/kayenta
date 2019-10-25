# Kayenta as a Standalone API

The purpose of this doc is to go over what you need to know about running Kayenta as a standalone microservice / API.

## Prerequisites to using Kayenta's API to do Canary Analysis 

The minimum requirements to have a usable API that can perform canary analysis are as follows.

### Redis

Kayenta runs [Orca](https://github.com/spinnaker/orca) embedded for creating pipelines of tasks that do the actual work of canary analysis. 
Redis is the data store that Orca uses to enable this to happen in a distributed manner.

Please note that Redis must not be in clustered mode.

Note: In AWS you can use ElastiCache, in a non-clustered Redis mode.

### A properly configured Kayenta configuration file.

See [Configuring Kayenta](./configuring-kayenta.md)

### The runnable Kayenta microservice

See [Building and Running Kayenta](./building-and-running-kayenta.md)

### An Application that has been instrumented to report metrics in a way usable by Kayenta

See [Instrumenting Application Metrics for Kayenta](./instrumenting-application-metrics-for-kayenta.md)

### Referee (optional)

If your organization is fine with hand jamming JSON canary configuration using the schema outlined in the [canary config doc](./canary-config.md) and parsing the canary results from the JSON returned by the Kayenta API then the following is optional.

If you want a UI for users that is designed for the Kayenta API but doesn't require the rest of Spinnaker then checkout [Referee](https://github.com/Nike-Inc/Referee). Referee gives users a config generation UI, a Retrospective analysis tool for rapidly iterating on canary config during the on-boarding process, and visual reports for the canary results produced by Kayenta.

## Performing Canary Analysis

Assuming you have worked through the [prerequisites](#prerequisites-to-using-kayentas-api-to-do-canary-analysis) then you should have a Kayenta environment up and running and ready to do canary analysis.

### A Brief overview of Kayenta's endpoints that are relevant to canary analysis

See the [full API docs](./faq.md#where-are-the-api-docs) and or the code for more detailed information.

#### /config

This endpoint allows users to store created canary config to be referenced by name and or id later when interacting with the canary or standalone canary endpoints

#### /canary

This is the main endpoint for canary analysis, this is the endpoint that Deck/Orca uses to [implement what you see in the deck UI as the canary analysis stage](https://github.com/spinnaker/orca/tree/master/orca-kayenta/src/main/kotlin/com/netflix/spinnaker/orca/kayenta).
A high-level overview of the way this endpoint works is as follows:

1. You trigger a canary analysis execution via a POST call to `/canary` with the following
 - canary config (can be an id reference to a saved config or the actual config just embedded in the call)
 - [scope / location data for your control and experiment](./instrumenting-application-metrics-for-kayenta.md)
 - Timestamps for the start and end times for the control and experiment that you are trying to analyze.
2. Kayenta starts the execution and returns an id
3. You can now poll GET `/canary/${id}` and wait for the status to be complete.
4. Once complete you can parse the results from the JSON.

See the [SignalFx End to End test for the /canary endpoint for a programmatic example](../kayenta-signalfx/src/integration-test/java/com/netflix/kayenta/signalfx/EndToEndCanaryIntegrationTests.java)

#### /standalone_canary_analysis

This endpoint is an abstraction on top of the `/canary` endpoint.
It is a port of the [Deck/Orca canary stage user experience in API form]((https://github.com/spinnaker/orca/tree/master/orca-kayenta/src/main/kotlin/com/netflix/spinnaker/orca/kayenta)).

Note: This endpoint is disabled by default you need to explicitly enable it via your [config](./configuring-kayenta.md)

It allows for a user to define a lifetime and interval length and do real-time canary analysis in multiple intervals and get an aggregated result.

A high-level overview of the way this endpoint works is as follows:
1. You trigger a canary analysis execution via a POST call to `/standalone_canary_analysis` with the following
    - canary config (can be an id reference to a saved config or the actual config just embedded in the call)
    - [scope / location data for your control and experiment](./instrumenting-application-metrics-for-kayenta.md)
    - a lifetime for the aggregated canary analysis
    - how often you want a `/canary` judgment to occur
    - fail fast score threshold
    - the final pass score threshold
2. Kayenta starts the execution and returns an id
3. You can now poll GET `/standalone_canary_analysis/${id}` and wait for the status to be complete.
4. Once complete you can parse the results from the JSON.

See the [SignalFx End to End test for the /standalone_canary_analysis endpoint for a programmatic example](../kayenta-signalfx/src/integration-test/java/com/netflix/kayenta/signalfx/EndToEndStandaloneCanaryAnalysisIntegrationTests.java)
