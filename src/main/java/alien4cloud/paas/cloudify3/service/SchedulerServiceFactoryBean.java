package alien4cloud.paas.cloudify3.service;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.beans.factory.FactoryBean;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class SchedulerServiceFactoryBean implements FactoryBean<ListeningScheduledExecutorService> {

    private final int poolSize;

    private final String poolName;

    public SchedulerServiceFactoryBean(String poolName, int poolSize) {
        this.poolName = poolName;
        this.poolSize = poolSize;
    }

    private static final AtomicInteger POOL_ID = new AtomicInteger(0);

    @Override
    public ListeningScheduledExecutorService getObject() throws Exception {
        BasicThreadFactory factory = new BasicThreadFactory.Builder()
                .namingPattern(poolName + "-" + POOL_ID.incrementAndGet() + "-%d")
                .build();
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(poolSize, factory);
        return MoreExecutors.listeningDecorator(executor);
    }

    @Override
    public Class<?> getObjectType() {
        return ListeningScheduledExecutorService.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
