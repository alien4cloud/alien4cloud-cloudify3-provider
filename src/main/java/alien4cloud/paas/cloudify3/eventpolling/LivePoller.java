package alien4cloud.paas.cloudify3.eventpolling;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;

/**
 * The live poller will start a single long running thead that will:
 * <ul>
 *     <li>Get the event stream at most each POLL_PERIOD seconds.</li>
 *     <li>Trigger schedule of it's delayed pollers to do the same in the future.</li>
 * </ul>
 */
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
     * Ideally, if event frequency is not too hight, an event request will be executed each POLL_PERIOD secondes.
     */
    private static final Duration POLL_PERIOD = Duration.ofSeconds(10);
	private static final int TIMEOUT = 5;

    /**
     * We definitely use one single thread for this poller.
     */
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

	public LivePoller(String url) {
        super(url);
    }

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
	public void triggerDelayedPollers(Instant from, Instant to) {
		for (DelayedPoller delayedPoller : delayedPollers) {
			delayedPoller.schedule(from, to);
		}
	}

    /**
     * This non blocking operation will start a long running thread that will get live event stream.
     */
    @Override
    public void start() {
        this.fromDate = Instant.now().minus(POLL_PERIOD);
        this.toDate = fromDate.plus(POLL_PERIOD);

        executorService.submit(() -> {
            // Start the long live thread
            while (true) {
                // Start the epoch polling
                try {
                    pollEpoch(fromDate, toDate);
                    triggerDelayedPollers(fromDate, toDate);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }

                // Move to next epoch
                fromDate = fromDate.plus(POLL_PERIOD);
                toDate = toDate.plus(POLL_PERIOD);
                Instant now = Instant.now();
                if (toDate.isAfter(now)) {
                    try {
                        Thread.sleep(toDate.getEpochSecond() - now.getEpochSecond() * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        });
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(TIMEOUT, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
