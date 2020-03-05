# Canary Config

The purpose of this doc is to describe the schema for Canary Configurations using [Markdown Syntax for Object Notation (MSON)]. 

## Canary Config Object model (object)

The canary config object is how users of the Kayenta API describe how they want Kayenta to compare the canary metrics against the baseline metrics for their applications.

### Properties

- `id` **some-custom-id** (string, optional) - You can supply a custom string here. If not supplied, a GUID will be generated for you. The id is used when you call Kayenta to trigger a canary execution, if you do not want to supply the config as part of the request.
- `name` **my-app golden signals canary config** (string, required) - Name for canary configuration.
- `description` **Canary config for my-app** (string, required) - Description for the canary configuration.
- `applications` (array[string], required) - A list of applications that the canary is for. You can have a list with single item `ad-hoc` as the entry, unless you are storing the configuration in Kayenta and sharing it.
- `judge` ([CanaryJudgeConfig](#CanaryJudgeConfig-object), required) - Judge configuration.
- `metrics` (array([CanaryMetricConfig](#CanaryMetricConfig-object))) - List of metrics to analyze.
- `templates` (map<string, string>, optional) - Templates allow you to compose and parameterize advanced queries against your telemetry provider. Parameterized queries are hydrated by values provided in the canary stage. The **project**, **resourceType**, **scope**, and **location** variable bindings are implicitly available. For example, you can interpolate **project** using the following syntax: **\${project}**.
- `classifier` ([CanaryClassifierConfig](#canaryclassifierconfig), required) - The classification configuration, such as group weights.

## CanaryJudgeConfig (object)

Currently there is one judge ([NetflixACAJudge-v1.0](https://github.com/spinnaker/kayenta/blob/master/kayenta-judge/src/main/scala/com/netflix/kayenta/judge/NetflixACAJudge.scala)) and this object should be static across the configuration (see the above examples).

### Properties

- `name` **NetflixACAJudge-v1.0** (string, required) - Judge to use, as of right now there is only `NetflixACAJudge-v1.0`.
- `judgeConfigurations` **{}** (object, required) - Map<string, object> of judgement configuration. As of right now, this should always be an empty object.

## CanaryMetricConfig (object)

Describes a metric that will be used in determining the health of a canary.

### Properties

- `name` **http errors** (string, required) - Human readable name of the metric under test.
- `query` (enum[[CanaryMetricSetQueryConfig](#CanaryMetricSetQueryConfig-object)], required) - Query config object for your metric source type.
- `groups` (array[string], required) - List of metrics groups that this metric will belong to.
- `analysisConfigurations` ([AnalysisConfiguration](#AnalysisConfiguration-object), required) - Analysis configuration, describes how to judge a given metric.
- `scopeName` (enum[string], required)
  - `default` - only accepted value here

## CanaryMetricSetQueryConfig (object)

Metric source interface for describing how to query for a given metric or metric source.

### Properties

- One of
  - AtlasCanaryMetricSetQueryConfig
  - DatadogCanaryMetricSetQueryConfig
  - GraphiteCanaryMetricSetQueryConfig
  - InfluxdbCanaryMetricSetQueryConfig
  - [NewRelicInsightsCanaryMetricSetQueryConfig](../kayenta-newrelic-insights/docs/metric-set-query-config.md)
  - PrometheusCanaryMetricSetQueryConfig
  - [SignalFxCanaryMetricSetQueryConfig](../kayenta-signalfx/docs/metric-set-query-config.md)
  - StackdriverCanaryMetricSetQueryConfig
  - WavefrontCanaryMetricSetQueryConfig

## AnalysisConfiguration (object)

Wrapper object that includes the Canary Analysis Configuration and describes how to judge a given metric.

### Properties

- `canary` ([CanaryAnalysisConfiguration](#CanaryAnalysisConfiguration-object))

## CanaryAnalysisConfiguration (object)

Describes how to judge a metric, see the [Netflix Automated Canary Analysis Judge] for more information.

### Properties

- `direction` (enum[string], required) - Which direction of statistical change triggers the metric to fail.
  - `increase` - Use when you want the canary to fail only if it is significantly higher than the baseline (error counts, memory usage, etc, where a decrease is not a failure).
  - `decrease` - Use when you want the canary to fail only if it is significantly lower than the baseline (success counts, etc, where a larger number is not a failure).
  - `either` - Use when you want the canary to fail if it is significantly higher or lower than the baseline.
- `nanStrategy` (enum[string], optional) - How to handle NaN values which can occur if the metric does not return data for a particular time interval.
  - `remove` (default) - Use when you expect a metric to always have data and you want the NaNs removed from your data set (usage metrics).
  - `replace` - Use when you expect a metric to return no data in certain use cases and you want the NaNs replaced with zeros (for example: count metrics, if no errors happened, then metric will return no data for that time interval).
- `critical` **true** (boolean, optional) - Use to fail the entire canary if this metric fails (recommended for important metrics that signal service outages or severe problems).
- `mustHaveData` **true** (boolean, optional) - Use to fail a metric if data is missing.
- `effectSize` ([EffectSize](#effectsize), optional) - Controls how much different the metric needs to be to fail or fail critically.
- `outliers` ([Outliers](#outliers), optional) - Controls how to classify and handle outliers.

## EffectSize

Controls the degree of statistical significance the metric needs to fail or fail critically. 
Metrics marked as critical can also define `criticalIncrease` and `criticalDecrease`. 
See the [Netflix Automated Canary Analysis Judge] and [Mann Whitney Classifier] classes for more information.

### Properties

- `allowedIncrease` **1.1** (number, optional) - Defaults to 1. The multiplier increase that must be met for the metric to fail. This example makes the metric fail when the metric has increased 10% from the baseline.
- `allowedDecrease` **0.90** (number, optional) - Defaults to 1. The multiplier decrease that must be met for the metric to fail. This example makes the metric fail when the metric has decreased 10% from the baseline.
- `criticalIncrease` **5.0** (number, optional) - Defaults to 1. The multiplier increase that must be met for the metric to be a critical failure and fail the entire analysis with a score of 0. This example make the canary fail critically if there is a 5x increase.
- `criticalDecrease` **0.5** (number, optional) - Defaults to 1. The multiplier decrease that must be met for the metric to be a critical failure and fail the entire analysis with a score of 0. This example make the canary fail critically if there is a 50% decrease.

## Outliers

Controls how to classify and handle outliers.
Outliers are determined based on the interquartile range (IQR) which is the middle 50% of the data.
Conceptually, the IQR measures where the bulk of the values lie within a distribution

The low outlier boundary is calculated using the equation: `Q1 - factor * IQR`.
The high outlier boundary is calculated using the equation: `Q3 + factor * IQR`. 
`Q1` and `Q3` are the data values of the lower and upper quartiles respectively.
`factor` is `outlierFactor` as explained below.
`IQR` is `Q3 - Q1`.

>Given a latency graph where `Q1` is 50 ms and `Q3` is 100 ms and the `factor` is 3:
`IQR` is `Q3 - Q1` which is 100 - 50 = 50.
The low outlier boundary is `Q1 - factor * IQR` which is `50 - 3 * 50 = -100`.
The high outlier boundary is `Q3 + factor * IQR` which is `100 + 3 * 50 = 250`. So for a data point to be
classified as an outlier in this example, it must be less than -100 ms or greater than 250 ms.

Here is a visual example:
![IQR Example](./assets/iqr-example.png)
<sup>The above image is a crop of <a href="https://en.wikipedia.org/wiki/Interquartile_range#/media/File:Boxplot_vs_PDF.svg">Boxplot_vs_PDF.svg</a> by <a href="https://en.wikipedia.org/wiki/User:Jhguch">Jhguch</a> at <a href="https://en.wikipedia.org/">en.wikipedia</a> licensed by <a href="https://creativecommons.org/licenses/by-sa/2.5">CC BY-SA 2.5</a>.</sup>

See the [Netflix Automated Canary Analysis Judge] and [Interquartile Range Detector] classes for more information.

### Properties

- `strategy` (enum[string], optional) - Remove or keep outliers.
  - `remove` - Use when you want to classify and remove outliers.
  - `keep` (default) - Use when you want to keep outliers.
- `outlierFactor` (number, optional) - Defaults to 3.0. The degree of significance a data point has to differ from other observations to be considered an outlier. If Q1 and Q3 are the lower and upper quartiles respectively then the values which fall below `Q1 - factor * IQR` or above `Q3 + factor * IQR` are considered outliers.

## CanaryClassifierConfig

### Properties

- `groupWeights` (enum[string], required)
  - `groups` **"Latency" : 33** (object, required) - List of each metrics group along with its corresponding weight. Weights must total 100.
  
## Links

- [Spinnaker Canary Best Practices]
- [Canary analysis: Lessons learned and best practices from Google and Waze]
- [Armory Kayenta Documentation]
- [Example Signalfx canary config](../kayenta-signalfx/docs/metric-set-query-config.md)
  
[Spinnaker Canary Best Practices]: https://www.spinnaker.io/guides/user/canary/best-practices/
[Armory Kayenta Documentation]: https://docs.armory.io/spinnaker/configure_kayenta/
[Markdown Syntax for Object Notation (MSON)]: https://github.com/apiaryio/mson
[Canary analysis: Lessons learned and best practices from Google and Waze]: https://cloud.google.com/blog/products/devops-sre/canary-analysis-lessons-learned-and-best-practices-from-google-and-waze
[Netflix Automated Canary Analysis Judge]: https://github.com/spinnaker/kayenta/blob/master/kayenta-judge/src/main/scala/com/netflix/kayenta/judge/NetflixACAJudge.scala
[Mann Whitney Classifier]: https://github.com/spinnaker/kayenta/blob/master/kayenta-judge/src/main/scala/com/netflix/kayenta/judge/classifiers/metric/MannWhitneyClassifier.scala
[Interquartile Range Detector]: https://github.com/spinnaker/kayenta/blob/master/kayenta-judge/src/main/scala/com/netflix/kayenta/judge/detectors/IQRDetector.scala
