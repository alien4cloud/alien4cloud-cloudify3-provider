package alien4cloud.paas.cloudify3.eventpolling;

import java.time.Duration;
import java.time.Instant;

import alien4cloud.paas.cloudify3.service.SchedulerServiceFactoryBean;

/**
 * An event poller is responsible to re-request events using a given interval but in the future.
 */
public class DelayedPoller extends AbstractCachedEventPoller {

    /**
     * This scheduler MUST only be used to schedule
     */
    private SchedulerServiceFactoryBean scheduler;

    /**
     * The delay this poller will wait until executing the query.
     */
    private Duration delay;

    public void schedule(Instant fromDate, Instant toDate) {
        // TODO: trigger a query for delayed concerns
    }

}
