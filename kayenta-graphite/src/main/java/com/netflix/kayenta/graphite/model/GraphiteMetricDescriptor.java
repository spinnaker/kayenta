/*
 * Copyright 2018 Snap Inc.
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

package com.netflix.kayenta.graphite.model;

import java.util.Collections;
import java.util.Map;

public class GraphiteMetricDescriptor {

  private final String name;
  private final Map<String, String> map;

  public GraphiteMetricDescriptor(String name) {
    this.name = name;
    this.map = Collections.singletonMap("name", name);
  }

  public String getName() {
    return name;
  }

  public Map<String, String> getMap() {
    return map;
  }
}
