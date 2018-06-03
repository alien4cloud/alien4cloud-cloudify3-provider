package alien4cloud.paas.cloudify3.eventpolling;

import alien4cloud.paas.cloudify3.util.DateUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * An delayed poller is responsible off re-requesting events in the future.
 */
@Setter
@Slf4j
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

    @Override
    public void start() {
        // Nothing to do here, polls will be started using schedule().
    }

    public void schedule(Instant fromDate, Instant toDate) {
        logDebug("Scheduling a polling for epoch {} -> {} in {} seconds", DateUtil.logDate(fromDate), DateUtil.logDate(toDate), delayInSeconds);
        scheduler.schedule(() -> {
            try {
                pollEpoch(fromDate, toDate);
            } catch (ExecutionException | InterruptedException e) {
                // TODO: handle correctly this exception
                log.error("TODO: handle correctly this exception", e);
            }
        }, delayInSeconds, TimeUnit.SECONDS);
    }

}
