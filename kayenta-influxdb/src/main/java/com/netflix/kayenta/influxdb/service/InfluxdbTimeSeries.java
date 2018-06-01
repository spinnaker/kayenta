/*
 * Copyright 2018 Armory, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.kayenta.influxdb.service;

import java.util.List;
import java.util.Map;

import lombok.Data;

//TODO possibly remove as replaced by InfluxdbResults
@Data
public class InfluxdbTimeSeries {
  private List<InfluxdbSeriesEntry> series;

  @Data
  static public class InfluxdbSeriesEntry {
    private String name;
    private List<String> columns;
    private Map<String, Object> values;

    //TODO: check if we need to adjust the values as has been done for datadog
    //TODO: update for possible multi query result (See https://docs.influxdata.com/influxdb/v1.5/guides/querying_data/#multiple-queries)
  }
}
