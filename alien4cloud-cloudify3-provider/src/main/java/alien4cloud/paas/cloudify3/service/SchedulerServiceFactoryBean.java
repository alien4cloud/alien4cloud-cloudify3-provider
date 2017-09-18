package alien4cloud.paas.cloudify3.service;

import java.util.concurrent.Executors;

import org.springframework.beans.factory.FactoryBean;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class SchedulerServiceFactoryBean implements FactoryBean<ListeningScheduledExecutorService> {

    private int poolSize = 4;

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    @Override
    public ListeningScheduledExecutorService getObject() throws Exception {
        return MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(poolSize));
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
