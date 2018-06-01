package alien4cloud.paas.cloudify3.eventpolling;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * Created by xdegenne on 01/06/2018.
 */
@Setter
@Getter
public class AbstractCachedEventPoller extends AbstractPoller {

    private Set<EventReference> eventCache;

}
