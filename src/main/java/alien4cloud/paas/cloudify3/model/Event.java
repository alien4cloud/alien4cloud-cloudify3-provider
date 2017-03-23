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

    private String eventType;

    @JsonProperty("@timestamp")
    private String timestamp;

    private String level;

    private EventContext context;

    private EventMessage message;

    private String executionId;

    @JsonIgnore
    public String getId() {
        StringJoiner buffer = new StringJoiner("_");
        buffer.add(eventType).add(timestamp);
        if (context != null) {
            buffer.add(context.getNodeId()).add(context.getOperation());
        }
        return buffer.toString();
    }
}