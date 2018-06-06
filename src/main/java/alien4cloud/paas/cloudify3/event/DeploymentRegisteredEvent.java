package alien4cloud.paas.cloudify3.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * This event is triggered when some deployments need to be monitored.
 */
@Getter
public class DeploymentRegisteredEvent extends ApplicationEvent {

    private final String[] deploymentIds;

    public DeploymentRegisteredEvent(Object source, String... deploymentIds) {
        super(source);
        this.deploymentIds = deploymentIds;
    }
}
