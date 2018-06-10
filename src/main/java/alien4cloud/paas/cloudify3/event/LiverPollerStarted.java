package alien4cloud.paas.cloudify3.event;

import alien4cloud.paas.cloudify3.service.model.CloudifySnapshot;
import lombok.Getter;

/**
 * The live poller starts it's job.
 */
@Getter
public class LiverPollerStarted extends CloudifyManagerEvent {

    private static final long serialVersionUID = -1126617350064097857L;

    public LiverPollerStarted(Object source) {
        super(source);
    }

}
