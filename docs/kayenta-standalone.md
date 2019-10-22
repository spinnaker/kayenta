# Kayenta as a Standalone API
The purpose of this doc is to go over what you need to know about running Kayenta as a standalone microservice / API.

## Prerequisites to using Kayenta's API to do Canary Analysis 
The minimum requirements to have a usable API that can perform canary analysis are as follows.
### Redis
Kayenta runs [Orca](https://github.com/spinnaker/orca) embedded for creating pipelines of tasks that do the actual work of canary analysis. 
Redis is the data store that Orca uses to enable this to happen in a distributed maner. 

Please note that Redis must not be in clustered mode.

Note: In AWS you can use ElastiCache, in a non-clustered redis mode.

### A properly configured kayenta configuration file.

See [Configuring Kayenta](./configuring-kayenta.md)

### The runnable Kayenta microservice

See [Building and Running Kayenta](./building-and-running-kayenta.md)

### An Application that has been instrumented to report metrics in a way usable by Kayenta

See [Instrumenting Application Metrics for Kayenta](./instrumenting-application-metrics-for-kayenta.md)

### Referee (optional)
If your organization is fine with hand jamming JSON canary configuration using the schema outlined in the [canary config doc](./canary-config.md) and parsing the canary results from the JSON returned by the Kayenta API then the following is optional.

If you want a UI for users that is designed for the Kayenta API but doesn't require the rest of Spinnaker then checkout [Referee](https://github.com/Nike-Inc/Referee). Referee gives users a config generation UI, a Retrospective analysis tool for rapidly iterating on canary config during the on-boarding process, and visual reports for the canary results produced by Kayenta.

## Performing Canary Analysis

Assuming you have worked through the [prerequisites](#prerequisites-to-using-kayentas-api-to-do-canary-analysis) then you should have a Kayenta environment up an running and ready to do canary analysis.

