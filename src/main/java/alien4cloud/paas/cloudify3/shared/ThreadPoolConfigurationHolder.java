package alien4cloud.paas.cloudify3.shared;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.function.Consumer;

/**
 * Created by xdegenne on 28/05/2018.
 */
//@Component
@Slf4j
@Deprecated
public class ThreadPoolConfigurationHolder implements Consumer<PluginConfiguration> {

    @Resource
    private PluginConfigurationHolder pluginConfigurationHolder;

    @Resource(name="cloudify-async-thread-pool")
    private ThreadPoolTaskExecutor mainThreadPoolTaskExecutor;

    @Resource(name="delayed-async-thread-pool")
    private ThreadPoolTaskExecutor delayedThreadPoolTaskExecutor;

    @PostConstruct
    public void init() {
        pluginConfigurationHolder.register(this);
    }

    @Override
    public void accept(PluginConfiguration pluginConfiguration) {
        int size = pluginConfiguration.getCloudifyAsyncThreadpoolCoreSize();
        if (size > 0 && mainThreadPoolTaskExecutor.getCorePoolSize() != size) {
            log.info("Setting cloudify-async-thread-pool coreSize to {}", size);
            mainThreadPoolTaskExecutor.setCorePoolSize(size);
        }
        size = pluginConfiguration.getCloudifyAsyncThreadpoolMaxSize();
        if (size >= mainThreadPoolTaskExecutor.getCorePoolSize() && mainThreadPoolTaskExecutor.getMaxPoolSize() != size) {
            log.info("Setting cloudify-async-thread-pool maxSize to {}", size);
            mainThreadPoolTaskExecutor.setMaxPoolSize(size);
        }
        size = pluginConfiguration.getDelayedAsyncThreadpoolCoreSize();
        if (size > 0 && delayedThreadPoolTaskExecutor.getCorePoolSize() != size) {
            log.info("Setting delayed-async-thread-pool coreSize to {}", size);
            delayedThreadPoolTaskExecutor.setCorePoolSize(size);
        }
        size = pluginConfiguration.getDelayedAsyncThreadpoolMaxSize();
        if (size >= delayedThreadPoolTaskExecutor.getCorePoolSize() && delayedThreadPoolTaskExecutor.getMaxPoolSize() != size) {
            log.info("Setting delayed-async-thread-pool maxSize to {}", size);
            delayedThreadPoolTaskExecutor.setMaxPoolSize(size);
        }
    }

}
