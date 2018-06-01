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

package com.netflix.kayenta.influxdb.orca;

import com.netflix.spinnaker.orca.pipeline.StageDefinitionBuilder;
import com.netflix.spinnaker.orca.pipeline.TaskNode;
import com.netflix.spinnaker.orca.pipeline.model.Stage;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component
public class InfluxdbFetchStage {
  @Bean
  StageDefinitionBuilder influxdbFetchStageBuilder() {
    return new StageDefinitionBuilder() {
      @Override
      public void taskGraph(@Nonnull Stage stage, @Nonnull TaskNode.Builder builder) {
        builder.withTask("influxdbFetch", InfluxdbFetchTask.class);
      }

      @Nonnull
      @Override
      public String getType() {
        return "influxdbFetch";
      }
    };
  }
}
