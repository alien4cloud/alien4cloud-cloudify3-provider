package alien4cloud.paas.cloudify3.eventpolling;

import alien4cloud.paas.cloudify3.service.SchedulerServiceFactoryBean;
import alien4cloud.paas.cloudify3.shared.EventDispatcher;
import alien4cloud.paas.cloudify3.util.SyspropConfig;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The configuration for the cfy manager dedicated child context.
 * For each instance of manager url, we'll instanciate a manager context.
 */
@ComponentScan(basePackages = { "alien4cloud.paas.cloudify3.eventpolling" })
@Slf4j
public class EventPollingConfig {

    // A static final index to identify pools in case of multiple instances.
    private static final AtomicInteger POOL_ID = new AtomicInteger(0);

    @Bean(name = "event-dispatcher")
    public EventDispatcher eventDispatcher() {
        return new EventDispatcher();
    }

    @Bean(name = "event-cache")
    @SneakyThrows
    public EventCache eventCache() {
        EventCache eventCache = new EventCache();
        return eventCache;
    }

    @Bean(name = "event-live-poller")
    @SneakyThrows
    public LivePoller livePoller() throws Exception {
        LivePoller livePoller = new LivePoller();
        return livePoller;
    }

    @Bean(name = "event-recovery-poller")
    @SneakyThrows
    public RecoveryPoller recoveryPoller() {
        RecoveryPoller recoveryPoller = new RecoveryPoller();
        return recoveryPoller;
    }

    @Bean(name = "event-delayed-30-poller")
    @SneakyThrows
    public DelayedPoller delayed30sPoller() {
        return new DelayedPoller(30);
    }

    @Bean(name = "event-delayed-300-poller")
    @SneakyThrows
    public DelayedPoller delayed5mnPoller() {
        return new DelayedPoller(300);
    }

    /**
     * Used by:
     * <ul>
     *     <li>The {@link EventCache} to manage TTL</li>
     *     <li>The 2 delayed pollers to schedule delayed epoch polling (blocking)</li>
     *     <li>The {@link RecoveryPoller} to submit the recovery epoch polling (blocking and potencially long run !)</li>
     * </ul>
     * @return
     */
    @Bean(name = "event-scheduler")
    public SchedulerServiceFactoryBean schedulerServiceFactoryBean() {
        return new SchedulerServiceFactoryBean("event-scheduler", SyspropConfig.getInt(SyspropConfig.EVENT_SCHEDULER_CORE_SIZE, 2));
    }

    @Bean(name= "event-async-thread-pool")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor(){
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setThreadNamePrefix("event-async-thread-pool-" + POOL_ID.incrementAndGet() + "-");
        threadPoolTaskExecutor.setCorePoolSize(SyspropConfig.getInt(SyspropConfig.EVENT_ASYNC_CORE_SIZE, 2));
        threadPoolTaskExecutor.setMaxPoolSize(SyspropConfig.getInt(SyspropConfig.EVENT_ASYNC_MAX_SIZE, 3));
        threadPoolTaskExecutor.setKeepAliveSeconds(SyspropConfig.getInt(SyspropConfig.EVENT_ASYNC_KEEPALIVESECONDS, 10));
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }
}
