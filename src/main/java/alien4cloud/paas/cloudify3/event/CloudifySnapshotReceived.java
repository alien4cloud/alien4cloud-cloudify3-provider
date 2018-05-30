package alien4cloud.paas.cloudify3.event;

import alien4cloud.paas.cloudify3.service.model.CloudifySnapshot;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * An snapshot has been received, it's the state of the cfy at a moment.
 */
@Getter
public class CloudifySnapshotReceived extends ApplicationEvent {

    private static final long serialVersionUID = -1126617350064097857L;

    private CloudifySnapshot cloudifySnapshot;

    public CloudifySnapshotReceived(Object source, CloudifySnapshot cloudifySnapshot) {
        super(source);
        this.cloudifySnapshot = cloudifySnapshot;
    }

}
