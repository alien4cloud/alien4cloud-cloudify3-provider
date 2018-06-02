package alien4cloud.paas.cloudify3.eventpolling;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import com.google.common.collect.Lists;

import alien4cloud.paas.cloudify3.util.DateUtil;
import lombok.extern.slf4j.Slf4j;

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
    private static final Duration POLL_PERIOD = Duration.ofSeconds(10);
	private static final int TIMEOUT = Integer.MAX_VALUE;

    private static final AtomicInteger POOL_ID = new AtomicInteger(0);

    /**
     * We definitely use one single thread for this poller.
     */
    private ExecutorService executorService;

    public LivePoller() {
        // initialize the 1 size thread pool
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
    public void start() {
        this.fromDate = Instant.now().minus(POLL_PERIOD);
        this.toDate = fromDate.plus(POLL_PERIOD);

        executorService.submit(() -> {
            // Start the long live thread
            while (true) {
                // Start the epoch polling
                if (log.isDebugEnabled()) {
                    log.debug("[{}] Beginning of live event polling, starting from {}", getPollerNature(), DateUtil.logDate(this.fromDate));
                }
                try {
                    pollEpoch(fromDate, toDate);
                    triggerDelayedPollers(fromDate, toDate);
                } catch (Exception e) {
                    e.printStackTrace();
                    // TODO: handle correctly this exception
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
                            log.debug("[{}] Sleeping {} ms before polling next epoch", getPollerNature(), sleepTime);
                        }
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        // TODO: handle correctly this exception
                        e.printStackTrace();
                    }
                } else if (log.isDebugEnabled()) {
                    log.debug("[{}] No sleep between epoch polling. A large number of coming events forces the system to poll events in real time.");
                }
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        // shutdown delayed pollers
        delayedPollers.forEach(DelayedPoller::shutdown);
        // shutdown long running thread
        executorService.shutdownNow();
    }
}
