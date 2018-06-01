package alien4cloud.paas.cloudify3.eventpolling;

import alien4cloud.paas.cloudify3.service.SchedulerServiceFactoryBean;
import com.google.common.collect.Lists;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Created by xdegenne on 01/06/2018.
 */
public class LivePoller extends AbstractCachedEventPoller {

    private ExecutorService executorService;

    private List<DelayedPoller> delayedPollers = Lists.newArrayList();

    public void addDelayedPoller(DelayedPoller delayedPoller) {
        delayedPollers.add(delayedPoller);
    }

    /**
     * Trigger scheduling for delayed pollers, non blocking.
     *
     * @param from
     * @param to
     */
    public void triggerDelayedPollers(Date from, Date to) {
        for (DelayedPoller delayedPoller : delayedPollers) {
            delayedPoller.schedule(from, to);
        }
    }

}
