package alien4cloud.paas.cloudify3.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventAlienWorkflowRelationshipStepEvent extends EventAlienWorkflow {
    private String targetNodeId;
    private String targetInstanceId;
}