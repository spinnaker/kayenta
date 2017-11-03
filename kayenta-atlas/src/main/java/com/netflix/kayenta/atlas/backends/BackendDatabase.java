/*
 * Copyright 2017 Netflix, Inc.
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

package com.netflix.kayenta.atlas.backends;

import com.netflix.kayenta.atlas.model.Backend;

import java.util.*;

public class BackendDatabase {

  private List<Backend> backends = new ArrayList<>();

  private boolean matches(Backend backend, String deployment, String dataset, String region, String environment) {
    // return false if it doesn't match the deployment.
    if (!backend.getDeployment().equals(deployment))
      return false;

    // return false if it doesn't match the dataset.
    if (!backend.getDataset().equals(dataset))
      return false;

    // return false if it doesn't match the region.
    if (backend.getRegions() != null && !backend.getRegions().contains(region))
      return false;

    // return false if it doesn't match the environment.
    if (backend.getEnvironments() != null && !backend.getEnvironments().contains(environment))
      return false;

    return true;
  }

  public synchronized Optional<Backend> getOne(String deployment, String dataset, String region, String environment) {
    return backends
      .stream()
      .filter(a -> matches(a, deployment, dataset, region, environment))
      .findFirst();
  }

  public synchronized void update(List<Backend> newBackends) {
    backends = newBackends;
  }
}
