package alien4cloud.paas.cloudify3.event;

import alien4cloud.paas.cloudify3.service.model.CloudifySnapshot;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * An snapshot has been received, it's the state of the cfy at a moment.
 */
@Getter
public class CloudifyManagerUnreachable extends CloudifyManagerEvent {

    private static final long serialVersionUID = -1126617350064097857L;

    public CloudifyManagerUnreachable(Object source) {
        super(source);
    }
}
