package alien4cloud.paas.cloudify3.model;

import java.util.Date;
import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter
@Setter
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.UnusedPrivateField")
public class Execution extends AbstractCloudifyModel {

    private String id;

    private Boolean isSystemWorkflow = false;

    private String blueprintId;

    private String workflowId;

    private String deploymentId;

    private String status;

    private String error;

    private Date createdAt;

    private Map<String, Object> parameters;
}
