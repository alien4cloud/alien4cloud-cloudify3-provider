package alien4cloud.paas.cloudify3.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Created by xdegenne on 02/06/2018.
 */
@Getter
public abstract class CloudifyManagerEvent extends ApplicationEvent {

    protected CloudifyManagerEvent(Object source) {
        super(source);
    }
}
