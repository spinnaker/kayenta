package com.netflix.kayenta.wavefront.orca;
        import com.netflix.spinnaker.orca.pipeline.StageDefinitionBuilder;
        import com.netflix.spinnaker.orca.pipeline.TaskNode;
        import com.netflix.spinnaker.orca.pipeline.model.Stage;
        import org.springframework.context.annotation.Bean;
        import org.springframework.stereotype.Component;

        import javax.annotation.Nonnull;

@Component
public class WavefrontFetchStage {

    @Bean
    StageDefinitionBuilder wavefrontSvcFetchStageBuilder(){
        return new StageDefinitionBuilder() {
            @Override
            public void taskGraph(@Nonnull Stage stage, @Nonnull TaskNode.Builder builder) {
                builder.withTask("wavefrontFetch", WavefrontFetchTask.class);
            }

            @Nonnull
            @Override
            public String getType() {
                return "wavefrontFetch";
            }
        };
    }
}
