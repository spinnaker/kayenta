### InfluxdbCanaryMetricSetQueryConfig (CanaryMetricSetQueryConfig)
Influxdb specific query configurations.

#### Properties
- `metricName` **measurement** (string, optional) - The measurement name where metrics are stored. This field is **required** UNLESS using `customInlineTemplate`.
- `fields` **count** (array[string], optional) - The list of field names that need to be included in query. This field is **required** UNLESS using `customInlineTemplate`.
- `customInlineTemplate` **SELECT sum("count") FROM measurement WHERE host = 'value1' AND ${scope} AND ${timeFilter} GROUP BY time(1m)** (string, optional) - This allows you to write your own IQL statement. `${scope}` and `{timeFilter}` variables are **required** in the IQL statement.
- `type` (enum[string], required)
    - `influxdb`

