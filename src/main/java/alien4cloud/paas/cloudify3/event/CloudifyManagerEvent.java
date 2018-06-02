package alien4cloud.paas.cloudify3.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * An event related to cfy manager instance.
 */
@Getter
public abstract class CloudifyManagerEvent extends ApplicationEvent {

    protected CloudifyManagerEvent(Object source) {
        super(source);
    }
}
