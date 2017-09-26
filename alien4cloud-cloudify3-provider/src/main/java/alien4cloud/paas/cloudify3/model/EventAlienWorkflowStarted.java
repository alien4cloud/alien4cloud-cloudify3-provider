package alien4cloud.paas.cloudify3.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter
@Setter
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventAlienWorkflowStarted {
    private String workflowName;
    private String subworkflow;
}