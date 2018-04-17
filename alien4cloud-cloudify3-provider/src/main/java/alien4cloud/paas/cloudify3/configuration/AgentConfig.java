package alien4cloud.paas.cloudify3.configuration;

import alien4cloud.ui.form.annotation.FormLabel;
import alien4cloud.ui.form.annotation.FormProperties;
import alien4cloud.ui.form.annotation.FormPropertyDefinition;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@FormProperties({ "user", "key" })
public class AgentConfig {

    @FormLabel("user")
    @FormPropertyDefinition(description = "The default ssh user to connect to VMs.", type = "string")
    private String user;

    @FormLabel("agent key path")
    @FormPropertyDefinition(description = "The path on the manager to the default ssh key for cloudify agents.", type = "string")
    private String key;
}
