package alien4cloud.paas.cloudify3.eventpolling;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.xml.bind.DatatypeConverter;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;

import alien4cloud.paas.cloudify3.model.Event;

/**
 * Created by xdegenne on 01/06/2018.
 */
public class LivePoller extends AbstractCachedEventPoller {

	private Instant fromDate;
	private Instant toDate;
	private static final int BATCH_SIZE = 100;
	private static final Duration INTERVAL = Duration.ofSeconds(10);
	private static final int TIMEOUT = 5;

	public LivePoller(String url) {
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		this.fromDate = Instant.now().minus(INTERVAL);
		this.toDate = fromDate.plus(INTERVAL);

		executorService.submit(() -> {
			while (true) {
				// Start the epoch polling
				try {
					pollEpoch(url, fromDate, toDate);
					triggerDelayedPollers(fromDate, toDate);
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}

				// Move to next epoch
				fromDate = fromDate.plus(INTERVAL);
				toDate = toDate.plus(INTERVAL);
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
		executorService.shutdown();
		try {
			executorService.awaitTermination(TIMEOUT, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	protected void pollEpoch(String url, Instant fromDate, Instant toDate)
			throws ExecutionException, InterruptedException {
		int offset = 0;
		while (true) {
			ListenableFuture<Event[]> future = getEventClient()
					.asyncGetBatch(url, Date.from(fromDate), Date.from(toDate), offset, LivePoller.BATCH_SIZE);
			// Get the events
			List<Event> events = Arrays.asList(future.get());

			if (events.isEmpty()) {
				break;
			}

			List<EventReference> refs = events.stream().map(e -> new EventReference(e.getId(),
					DatatypeConverter.parseDateTime(e.getTimestamp()).getTimeInMillis())).collect(Collectors.toList());

			// The list of id added into cache
			Set<String> whiteList = refs.stream().map(r -> {
				if (getEventCache().add(r)) {
					return r.id;
				} else {
					return null;
				}
			}).filter(Objects::nonNull).collect(Collectors.toSet());

			// Dispatch the events
			events.removeIf(e -> !whiteList.contains(e.getId()));
			getEventDispatcher().dispatch(Date.from(fromDate), events.toArray(new Event[0]), "live stream");

			// Increment the offset
			if (events.size() < BATCH_SIZE) {
				// Finish this epoch
				break;
			} else {
				offset += BATCH_SIZE;
			}
		}

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

}
