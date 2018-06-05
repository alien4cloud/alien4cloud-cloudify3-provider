package alien4cloud.paas.cloudify3.eventpolling;

import alien4cloud.paas.cloudify3.util.DateUtil;
import alien4cloud.paas.cloudify3.util.SyspropConfig;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The live poller will start a single long running thread that will:
 * <ul>
 *     <li>Get the event stream at at most each POLL_PERIOD seconds.</li>
 *     <li>Trigger schedule of it's delayed pollers to do the same in the future.</li>
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
     */
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(SyspropConfig.getInt(SyspropConfig.LIVEPOLLER_POLL_INTERVAL_IN_SECONDS, 15));

    private static final AtomicInteger POOL_ID = new AtomicInteger(0);

    /**
     * We definitely use one single thread for this poller.
     */
    private ExecutorService executorService;

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

    private List<DelayedPoller> delayedPollers = Lists.newArrayList();

	public void addDelayedPoller(DelayedPoller delayedPoller) {
		delayedPollers.add(delayedPoller);
	}

    @Override
    public void setUrl(String url) {
        super.setUrl(url);
        delayedPollers.forEach(delayedPoller -> delayedPoller.setUrl(url));
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

        this.toDate = Instant.now();
        this.fromDate = toDate.minus(POLL_INTERVAL);

        executorService.submit(() -> {
            // Start the long live thread
            while (true) {
                // Start the epoch polling

                if (log.isDebugEnabled()) {
                    logDebug("Beginning of live event polling, starting from {}", DateUtil.logDate(fromDate));
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
        });
    }

    @PreDestroy
    public void shutdown() {
        // shutdown long running thread
        executorService.shutdownNow();
    }
}
