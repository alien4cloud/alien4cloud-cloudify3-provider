package alien4cloud.paas.cloudify3.configuration;

import alien4cloud.ui.form.annotation.FormPropertyConstraint;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class KubernetesConfiguration {

    private List<String> imports;

    @FormPropertyConstraint(pattern = "http\\:.+(?:\\d+)")
    private String kubernetesUrl;
}
