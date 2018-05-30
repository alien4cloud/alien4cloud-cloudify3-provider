package alien4cloud.paas.cloudify3.service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.cloudify3.configuration.CloudConfigurationHolder;
import alien4cloud.paas.cloudify3.configuration.MappingConfigurationHolder;
import alien4cloud.paas.cloudify3.event.CloudifySnapshotReceived;
import alien4cloud.paas.cloudify3.model.*;
import alien4cloud.paas.cloudify3.restclient.DeploymentClient;
import alien4cloud.paas.cloudify3.restclient.ExecutionClient;
import alien4cloud.paas.cloudify3.restclient.NodeClient;
import alien4cloud.paas.cloudify3.restclient.NodeInstanceClient;
import alien4cloud.paas.cloudify3.service.model.CloudifySnapshot;
import alien4cloud.paas.cloudify3.util.DateUtil;
import alien4cloud.paas.cloudify3.util.FutureUtil;
import alien4cloud.paas.model.*;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.utils.MapUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.normative.ToscaNormativeUtil;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * In charge of snapshoting cfy status at startup, and feed a4c status in consequences.
 */
@org.springframework.stereotype.Service("cloudify-sync-service")
@Slf4j
public class SyncService {

    @Resource
    private ExecutionClient executionClient;

    @Resource
    private NodeInstanceClient nodeInstanceClient;

    @Resource
    private NodeClient nodeClient;

    @Resource
    private DeploymentClient deploymentClient;

    @Resource
    private CloudConfigurationHolder cloudConfigurationHolder;

    @Resource
    private RuntimePropertiesService runtimePropertiesService;

    @Resource(name = "cloudify-async-thread-pool")
    private ThreadPoolTaskExecutor restPool;

    @Resource(name="cloudify-scheduler")
    private ListeningScheduledExecutorService scheduler;

    @Autowired
    private ApplicationEventPublisher bus;

    /**
     * Snapshots the state of cfy for other services.
     */
    @PostConstruct
    public void init() {

        CloudifySnapshot cloudifySnapshot = new CloudifySnapshot();
        // TODO: Do the necessary stuff ...
        // deploymentClient.asyncList() then executionClient.asyncList()
        bus.publishEvent(new CloudifySnapshotReceived(this, cloudifySnapshot));
    }


}
