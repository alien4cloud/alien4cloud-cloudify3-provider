package alien4cloud.paas.cloudify3.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter
@Setter
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventAlienWorkflow {

    public static final String STAGE_IN = "in";
    public static final String STAGE_OK = "ok";

    private String stage;
    private String stepId;
    private String operationName;
}