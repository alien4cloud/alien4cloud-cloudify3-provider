package alien4cloud.paas.cloudify3.model;

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
    @JsonIgnore
    private String id;

    private String eventType;

    @JsonProperty("@timestamp")
    private String timestamp;

    private String level;

    private EventContext context;

    private EventMessage message;

    @JsonIgnore
    public String getId() {
        if (id == null) {
            StringBuilder buffer = new StringBuilder(eventType).append("_").append(timestamp);
            if (context != null) {
                buffer.append(context.getExecutionId()).append("_").append(context.getNodeId()).append("_").append(context.getOperation());
            }
            id = buffer.toString();
        }
        return id;
    }
}