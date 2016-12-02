package alien4cloud.paas.cloudify3.configuration;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class KubernetesConfiguration {

    private List<String> imports;

    private String kubernetesDeploymentId = "kubernetes";
}
