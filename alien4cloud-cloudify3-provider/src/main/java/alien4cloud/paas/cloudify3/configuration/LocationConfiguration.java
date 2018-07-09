package alien4cloud.paas.cloudify3.configuration;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.ui.form.annotation.FormProperties;

@Getter
@Setter
@FormProperties({ "dsl", "imports", "agentConfig" })
public class LocationConfiguration {

    private String dsl;

    private List<String> imports;

    private AgentConfig agentConfig;
}
