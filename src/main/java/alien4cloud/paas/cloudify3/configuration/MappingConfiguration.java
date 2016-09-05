package alien4cloud.paas.cloudify3.configuration;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MappingConfiguration {

    private String dslVersion;

    private String generatedTypePrefix;

    private String generatedNodePrefix;

    private String generatedArtifactPrefix;

    private String nativePropertyParent;

    private String artifactDirectoryName;

    private String implementationArtifactDirectoryName;

    private Map<String, String> normativeTypes;

    private Relationships relationships;

    @Getter
    @Setter
    public static class Relationships {

        private LifeCycle lifeCycle;

        @Getter
        @Setter
        public static class LifeCycle {

            private Map<String, String> source;

            private Map<String, String> target;
        }
    }

}
