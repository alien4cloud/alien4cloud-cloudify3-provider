package alien4cloud.paas.cloudify3.model;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class Event extends AbstractCloudifyModel {
    /** Can be cloudify_log or cloudify_event */
    private String type;
    /** workflow_started etc.. */
    private String eventType;
    @JsonProperty("@timestamp")
    private String timestamp;
    private String level;
    private EventContext context;
    private EventMessage message;

    @JsonIgnore
    public String getId() {
        StringBuilder buffer = new StringBuilder().append(eventType).append("::").append(timestamp);
        if (context != null) {
            buffer.append(context.getExecutionId()).append("::").append(context.getNodeId()).append("::").append(context.getOperation());
        }
        return buffer.toString();
    }
}