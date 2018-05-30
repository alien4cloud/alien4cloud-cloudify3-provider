package alien4cloud.paas.cloudify3.event;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

/**
 * An event indicating the initiation cloudify error
 */
@Getter
public class CloudifyInitiationError extends ApplicationEvent {

    private Throwable error;

    public CloudifyInitiationError(Object source, Throwable error) {
        super(source);
        this.error = error;
    }
}
