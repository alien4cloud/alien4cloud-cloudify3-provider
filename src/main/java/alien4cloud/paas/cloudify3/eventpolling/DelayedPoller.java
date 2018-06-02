package alien4cloud.paas.cloudify3.eventpolling;

import lombok.Setter;

import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * An delayed poller is responsible off re-requesting events in the future.
 */
@Setter
public class DelayedPoller extends AbstractPoller {

    /**
     * Used to schedule polls in the given delayInSeconds.
     */
    private ScheduledExecutorService scheduler;

    /**
     * The delayInSeconds this poller will wait until executing the query.
     */
    private long delayInSeconds;

    public DelayedPoller(long delayInSeconds) {
        this.delayInSeconds = delayInSeconds;
    }

    @Override
    public String getPollerNature() {
        return delayInSeconds + "s delayed stream";
    }

    public void schedule(Instant fromDate, Instant toDate) {
        scheduler.schedule(() -> {
            try {
                pollEpoch(fromDate, toDate);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, delayInSeconds, TimeUnit.SECONDS);
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
