package alien4cloud.paas.cloudify3.eventpolling;

import lombok.Setter;

import javax.annotation.PreDestroy;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Just a bean to manage the tear down of the dedicated scheduler.
 */
@Setter
public class SchedulerWrapper {

    private ScheduledExecutorService scheduler;

    @PreDestroy
    public void stop() {
        scheduler.shutdownNow();
    }

}
