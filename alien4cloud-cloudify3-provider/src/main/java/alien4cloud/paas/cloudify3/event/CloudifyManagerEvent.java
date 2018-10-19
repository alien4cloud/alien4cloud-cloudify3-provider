package alien4cloud.paas.cloudify3.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public abstract class CloudifyManagerEvent extends ApplicationEvent {
    protected CloudifyManagerEvent(Object source) {
        super(source);
    }
}
