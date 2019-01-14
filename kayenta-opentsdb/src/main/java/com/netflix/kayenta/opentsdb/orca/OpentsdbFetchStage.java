package com.netflix.kayenta.opentsdb.orca;

import com.netflix.spinnaker.orca.pipeline.StageDefinitionBuilder;
import com.netflix.spinnaker.orca.pipeline.TaskNode;
import com.netflix.spinnaker.orca.pipeline.model.Stage;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component
public class OpentsdbFetchStage {

  @Bean
  StageDefinitionBuilder openTsdbFetchStageBuilder(){
    return new StageDefinitionBuilder() {
      @Override
      public void taskGraph(@Nonnull Stage stage, @Nonnull TaskNode.Builder builder) {
        builder.withTask("openTsdbFetch", OpentsdbFetchTask.class);
      }

      @Nonnull
      @Override
      public String getType() {
        return "opentsdbFetch";
      }
    };
  }
}
