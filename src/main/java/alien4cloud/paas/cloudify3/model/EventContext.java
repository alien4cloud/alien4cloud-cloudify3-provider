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
public class EventContext extends AbstractCloudifyModel {

    private String taskId;

    private String blueprintId;

    private String plugin;

    private String taskTarget;

    private String nodeName;

    private String workflowId;

    private String nodeId;

    private String taskName;

    private String operation;

    private String deploymentId;

    private String executionId;
}
