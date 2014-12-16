package alien4cloud.paas.cloudify3;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import alien4cloud.model.application.DeploymentSetup;
import alien4cloud.paas.AbstractPaaSProvider;
import alien4cloud.paas.IConfigurablePaaSProvider;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.cloudify3.configuration.CloudConfiguration;
import alien4cloud.paas.cloudify3.configuration.CloudConfigurationHolder;
import alien4cloud.paas.cloudify3.error.OperationNotSupportedException;
import alien4cloud.paas.cloudify3.service.ComputeTemplateMatcherService;
import alien4cloud.paas.cloudify3.service.DeploymentService;
import alien4cloud.paas.cloudify3.service.EventService;
import alien4cloud.paas.cloudify3.service.StatusService;
import alien4cloud.paas.cloudify3.service.model.AlienDeployment;
import alien4cloud.paas.cloudify3.service.model.MatchedPaaSComputeTemplate;
import alien4cloud.paas.exception.OperationExecutionException;
import alien4cloud.paas.exception.PluginConfigurationException;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.InstanceInformation;
import alien4cloud.paas.model.NodeOperationExecRequest;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.tosca.container.model.topology.Topology;
import alien4cloud.tosca.model.PropertyDefinition;

/**
 * The cloudify 3 PaaS Provider implementation
 *
 * @author Minh Khang VU
 */
@Slf4j
public class CloudifyPaaSProvider extends AbstractPaaSProvider implements IConfigurablePaaSProvider<CloudConfiguration> {

    @Resource
    private DeploymentService deploymentService;

    @Resource
    private CloudConfigurationHolder cloudConfigurationHolder;

    @Resource
    private EventService eventService;

    @Resource
    private StatusService statusService;

    @Resource
    private ComputeTemplateMatcherService computeTemplateMatcherService;

    /**
     * ********************************************************************************************************************
     * *****************************************************Deployment*****************************************************
     * ********************************************************************************************************************
     */

    @Override
    protected void doDeploy(String deploymentName, String deploymentId, Topology topology, List<PaaSNodeTemplate> computes,
            Map<String, PaaSNodeTemplate> nodes, DeploymentSetup deploymentSetup) {
        List<MatchedPaaSComputeTemplate> matchedComputes = computeTemplateMatcherService.match(computes, deploymentSetup);
        AlienDeployment deployment = new AlienDeployment(deploymentId, deploymentName, topology, matchedComputes, nodes);
        deploymentService.deploy(deployment);
    }

    @Override
    public void undeploy(String deploymentId) {
        deploymentService.undeploy(deploymentId);
    }

    /**
     * ********************************************************************************************************************
     * *****************************************************Configurations*************************************************
     * ********************************************************************************************************************
     */

    @Override
    public CloudConfiguration getDefaultConfiguration() {
        return cloudConfigurationHolder.getConfiguration();
    }

    @Override
    public void setConfiguration(CloudConfiguration newConfiguration) throws PluginConfigurationException {
        cloudConfigurationHolder.setConfiguration(newConfiguration);
    }

    /**
     * ********************************************************************************************************************
     * *****************************************************Events*********************************************************
     * ********************************************************************************************************************
     */

    @Override
    public void getEventsSince(Date lastTimestamp, int batchSize, IPaaSCallback<AbstractMonitorEvent[]> eventsCallback) {
        eventService.getEventsSince(lastTimestamp, batchSize);
    }

    /**
     * ********************************************************************************************************************
     * *****************************************************Status*********************************************************
     * ********************************************************************************************************************
     */

    @Override
    public DeploymentStatus getStatus(String deploymentId) {
        return statusService.getStatus(deploymentId);
    }

    @Override
    public DeploymentStatus[] getStatuses(String[] deploymentIds) {
        return statusService.getStatuses(deploymentIds);
    }

    @Override
    public Map<String, Map<String, InstanceInformation>> getInstancesInformation(String deploymentId, Topology topology) {
        return statusService.getInstancesInformation(deploymentId, topology);
    }

    /**
     * ********************************************************************************************************************
     * *****************************************************Not implemented operation**************************************
     * ********************************************************************************************************************
     */

    @Override
    public void scale(String deploymentId, String nodeTemplateId, int instances) {
        throw new OperationNotSupportedException("scale is not supported yet");
    }

    @Override
    public Map<String, String> executeOperation(String deploymentId, NodeOperationExecRequest nodeOperationExecRequest) throws OperationExecutionException {
        throw new OperationNotSupportedException("executeOperation is not supported yet");
    }

    @Override
    public Map<String, PropertyDefinition> getDeploymentPropertyMap() {
        return null;
    }
}