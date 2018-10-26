package alien4cloud.paas.cloudify3.shared;


import alien4cloud.paas.cloudify3.configuration.CloudConfiguration;
import alien4cloud.paas.cloudify3.eventpolling.EventPollingConfig;
import alien4cloud.utils.ClassLoaderUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class EventServiceInstanceFactory {

    @Resource
    private ApplicationContext mainContext;

    EventServiceInstance buildEventService(String url, CloudConfiguration cloudConfiguration) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        context.setParent(mainContext);
        context.setClassLoader(mainContext.getClassLoader());
        ClassLoaderUtil.runWithContextClassLoader(mainContext.getClassLoader(), () -> {
            context.register(EventPollingConfig.class);
            context.refresh();
        });

        return new EventServiceInstance(context,url,cloudConfiguration);
    }
}
