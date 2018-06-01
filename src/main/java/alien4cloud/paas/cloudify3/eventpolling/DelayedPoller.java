package alien4cloud.paas.cloudify3.eventpolling;

import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * An event poller is responsible to re-request events using a given interval but in the future.
 */
public class DelayedPoller extends AbstractPoller {

    /**
     * This scheduler MUST only be used to schedule
     */
    private ScheduledExecutorService scheduler;

    /**
     * The delay this poller will wait until executing the query.
     */
    private long delay;

    public DelayedPoller(String url, long delay) {
        super(url);
        this.delay = delay;
    }

    public void schedule(Instant fromDate, Instant toDate) {
        scheduler.schedule(() -> {
            try {
                pollEpoch(fromDate, toDate);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, delay, TimeUnit.SECONDS);
    }

    @Override
    public void start() {
        // Nothing to do here. The polls will be scheduled.
    }

    @Override
    public void shutdown() {
        // Nothing to shutdown here (the scheduler is managed elsewhere).
    }
}
