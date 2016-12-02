package alien4cloud.paas.cloudify3.configuration;

import alien4cloud.ui.form.annotation.FormProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@FormProperties({ "dsl", "imports", "kubernetesDeploymentId" })
public class KubernetesLocationConfiguration extends LocationConfiguration {

    private String kubernetesDeploymentId = "kubonly";

}
