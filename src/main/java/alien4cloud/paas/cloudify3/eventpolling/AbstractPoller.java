package alien4cloud.paas.cloudify3.eventpolling;

import alien4cloud.paas.cloudify3.shared.EventClient;
import alien4cloud.paas.cloudify3.shared.EventDispatcher;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * Created by xdegenne on 01/06/2018.
 */
@Setter
@Getter
public abstract class AbstractPoller {

    private EventClient eventClient;

    private EventDispatcher eventDispatcher;

    public abstract void start();

}
