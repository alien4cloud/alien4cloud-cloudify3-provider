package alien4cloud.paas.cloudify3.configuration;

import alien4cloud.ui.form.annotation.FormLabel;
import alien4cloud.ui.form.annotation.FormProperties;
import alien4cloud.ui.form.annotation.FormPropertyDefinition;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@FormProperties({ "dsl", "imports", "agentConfig", "managerNetworkName", "securityGroupName", "openstackConfig" })
public class OpenstackLocationConfiguration extends LocationConfiguration {

    @FormLabel("manager network name")
    @FormPropertyDefinition(description = "The name of the Cloudify manager network.", type = "string", isRequired = true)
    private String managerNetworkName;

    @FormLabel("security group name")
    @FormPropertyDefinition(description = "The name of the default security group to attach to the VM's agent.", type = "string")
    private String securityGroupName;

    private OpenstackConfig openstackConfig;

}
