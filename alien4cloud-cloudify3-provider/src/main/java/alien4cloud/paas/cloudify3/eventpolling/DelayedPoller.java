package alien4cloud.paas.cloudify3.eventpolling;

import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import alien4cloud.paas.cloudify3.util.DateUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import javax.annotation.Resource;

/**
 * An delayed poller is responsible off re-requesting events in the future.
 */
@Setter
@Slf4j
public class DelayedPoller extends AbstractPoller {

    /**
     * Used to schedule polls in the given delayInSeconds.
     */
    @Resource(name = "event-scheduler")
    private ScheduledExecutorService scheduler;

    /**
     * The delayInSeconds this poller will wait until executing the query.
     */
    private long delayInSeconds;

    private AtomicLong _dispatchedEvent = new AtomicLong(0);

    private AtomicLong _lastLoggedTime = new AtomicLong(0);

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

    @Override
    protected Logger getLogger() {
        return log;
    }

    public void schedule(Instant fromDate, Instant toDate) {
        logDebug("Scheduling a polling for epoch {} -> {} in {} seconds", DateUtil.logDate(fromDate), DateUtil.logDate(toDate), delayInSeconds);
        scheduler.schedule(() -> {
            try {
                PollResult result = pollEpoch(fromDate, toDate);
                _dispatchedEvent.addAndGet(result.eventDispatchedCount);

                // just to know if DelayedPoller is useless or not
                long currentTime = System.currentTimeMillis();
                if (currentTime > _lastLoggedTime.get() + 1000 * 60 * 60) {
                    _lastLoggedTime.set(currentTime);
                    // each hour log this information
                    logInfo("{} events dispatched since system startup", _dispatchedEvent);
                }
            } catch (PollingException e) {
                // TODO: manage disaster recovery
                logError("Giving up polling after several retries", e);
            } catch (Exception e) {
                log.error("Fatal error occurred: ", e);
            }
        }, delayInSeconds, TimeUnit.SECONDS);
    }
}
