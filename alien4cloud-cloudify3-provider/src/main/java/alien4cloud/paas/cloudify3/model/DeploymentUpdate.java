package alien4cloud.paas.cloudify3.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.UnusedPrivateField")
public class DeploymentUpdate extends AbstractCloudifyModel {

    private String id;

    private String blueprintId;

    private String state;

    private String executionId;
}
