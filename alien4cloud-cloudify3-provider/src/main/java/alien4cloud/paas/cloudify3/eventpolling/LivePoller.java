package alien4cloud.paas.cloudify3.eventpolling;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;

import alien4cloud.paas.cloudify3.event.LiverPollerStarted;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import com.google.common.collect.Lists;

import alien4cloud.paas.cloudify3.util.DateUtil;
import alien4cloud.paas.cloudify3.util.SyspropConfig;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * LivePoller is responsible for getting the realtime events stream from Cfy.
 * <p/>
 * It will start a single long running thread that will:
 * <ul>
 * <li>Get the event stream at at most each POLL_PERIOD seconds querying POLL_INTERVAL with a sliding window.</li>
 * <li>Trigger schedule of it's delayed pollers to do the same in the future.</li>
 * </ul>
 */
@Slf4j
public class LivePoller extends AbstractPoller {

    /**
     * The {fromDate} of the event request.
     */
    private Instant fromDate;

    /**
     * The {toDate} of the event request.
     */
    private Instant toDate;

    /**
     * Ideally, if event frequency is not too high, an event request will be executed each POLL_PERIOD seconds.
     */
    private static final Duration POLL_PERIOD = Duration.ofSeconds(SyspropConfig.getInt(SyspropConfig.LIVEPOLLER_POLL_PERIOD_IN_SECONDS, 10));

    /**
     * The interval that will be requested, should be > POLL_PERIOD && < POLL_PERIOD * 2 to avoid event misses.
     * Shouldn't be < POLL_PERIOD (for sure we'll miss events).
     */
    protected static final Duration POLL_INTERVAL = Duration.ofSeconds(SyspropConfig.getInt(SyspropConfig.LIVEPOLLER_POLL_INTERVAL_IN_SECONDS, 15));

    private static final AtomicInteger POOL_ID = new AtomicInteger(0);

    /**
     * We definitely use one single thread for this poller.
     */
    private ExecutorService executorService;

    /**
     * Delayed pollers.
     */
    private List<DelayedPoller> delayedPollers = Lists.newArrayList();

    @Autowired
    private ApplicationEventPublisher bus;


    public LivePoller() {
        // initialize the 1 size thread pool, never change this if your are not sure about what your are doing !
        BasicThreadFactory factory = new BasicThreadFactory.Builder()
                .namingPattern("event-live-polling-" + POOL_ID.incrementAndGet() + "-%d")
                .build();
        executorService = Executors.newSingleThreadExecutor(factory);
    }

    @Override
    public String getPollerNature() {
        return "Live stream";
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    public void setUrl(String url) {
        super.setUrl(url);
        delayedPollers.forEach(delayedPoller -> delayedPoller.setUrl(url));
    }

    public void addDelayedPoller(DelayedPoller delayedPoller) {
        delayedPollers.add(delayedPoller);
    }

    /**
     * Trigger scheduling for delayed pollers, non blocking.
     *
     * @param from
     * @param to
     */
    private void triggerDelayedPollers(Instant from, Instant to) {
        delayedPollers.forEach(delayedPoller -> delayedPoller.schedule(from, to));
    }

    /**
     * This non blocking operation will start a long running thread that will get live event stream.
     */
    @Override
    public void start() {
        executorService.submit(() -> {
            try {
                // wait POLL_INTERVAL / 2 before starting live polling (avoid event duplication in case of quick restart)
                logInfo("Sleeping before starting working ...", POLL_INTERVAL.toMillis() / 2);
                Thread.sleep(POLL_INTERVAL.toMillis() / 2);
                this.toDate = Instant.now();
                this.fromDate = toDate.minus(POLL_INTERVAL);
                livePoll();
            } catch (Exception e) {
                log.error("Fatal error occurred: ", e);
            }
        });
    }

    private void livePoll() {
        logInfo("Starting live polling now !");
        bus.publishEvent(new LiverPollerStarted(this));
        // Start the long live thread
        while (true) {
            if (log.isDebugEnabled()) {
                logDebug("Beginning of live event polling, starting from {} to {}", DateUtil.logDate(fromDate), DateUtil.logDate(toDate));
            }
            try {
                pollEpoch(fromDate, toDate);
                triggerDelayedPollers(fromDate, toDate);
            } catch (PollingException e) {
                // TODO: manage disaster recovery
                logError("Giving up polling after several retries", e);
                return;
            }
            // Move to next epoch
            fromDate = fromDate.plus(POLL_PERIOD);
            toDate = toDate.plus(POLL_PERIOD);
            Instant now = Instant.now();
            if (toDate.isAfter(now)) {
                try {
                    long sleepTime = (toDate.getEpochSecond() - now.getEpochSecond()) * 1000;
                    if (log.isDebugEnabled()) {
                        logDebug("Sleeping {} ms before polling next epoch", sleepTime);
                    }
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    // TODO: handle correctly this exception
                    log.error("TODO: handle correctly this exception", e);
                }
            } else if (log.isDebugEnabled()) {
                logDebug("No sleep between epoch polling. A large number of coming events forces the system to poll events in real time.");
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        // shutdown long running thread
        executorService.shutdownNow();
    }
}
