package alien4cloud.paas.cloudify3.shared.model;

import alien4cloud.paas.cloudify3.model.Event;
import lombok.Getter;
import lombok.Setter;

/**
 * A generic log batch as received from the log server.
 */
@Getter
@Setter
public class LogBatch {
    private Long id;
    private Event[] entries;
}
