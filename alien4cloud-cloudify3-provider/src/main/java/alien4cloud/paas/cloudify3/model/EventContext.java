package alien4cloud.paas.cloudify3.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import alien4cloud.paas.cloudify3.util.PermissiveStringDeserializer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.UnusedPrivateField")
public class EventContext extends AbstractCloudifyModel {
    private String blueprintId;
    private String deploymentId;
    private String workflowId;
    private String executionId;

    private String taskId;
    private String taskName;
    private String taskTarget;
    private String plugin;

    private String nodeId;
    private String nodeName;

    // for relationships tasks
    private String sourceId;
    private String sourceName;
    private String targetId;
    private String targetName;

    @JsonDeserialize(using = PermissiveStringDeserializer.class)
    private String operation;
    @JsonDeserialize(using = PermissiveStringDeserializer.class)
    private String taskQueue;
    @JsonDeserialize(using = PermissiveStringDeserializer.class)
    private String taskErrorCauses;
}
