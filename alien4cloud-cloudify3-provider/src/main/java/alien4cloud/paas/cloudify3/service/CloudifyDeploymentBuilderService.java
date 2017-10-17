package alien4cloud.paas.cloudify3.service;

import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.INSTALL;
import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.UNINSTALL;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.templates.ServiceNodeTemplate;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.model.workflow.NodeWorkflowStep;
import org.alien4cloud.tosca.model.workflow.RelationshipWorkflowStep;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.alien4cloud.tosca.normative.ToscaNormativeUtil;
import org.alien4cloud.tosca.normative.constants.NormativeRelationshipConstants;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.model.components.IndexedModelUtils;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationResources;
import alien4cloud.orchestrators.locations.services.ILocationResourceService;
import alien4cloud.paas.cloudify3.configuration.CfyConnectionManager;
import alien4cloud.paas.cloudify3.error.SingleLocationRequiredException;
import alien4cloud.paas.cloudify3.model.DeploymentPropertiesNames;
import alien4cloud.paas.cloudify3.service.model.CloudifyDeployment;
import alien4cloud.paas.cloudify3.service.model.HostWorkflow;
import alien4cloud.paas.cloudify3.service.model.Relationship;
import alien4cloud.paas.cloudify3.service.model.StandardWorkflow;
import alien4cloud.paas.cloudify3.service.model.WorkflowStepLink;
import alien4cloud.paas.cloudify3.service.model.Workflows;
import alien4cloud.paas.cloudify3.util.mapping.PropertiesMappingUtil;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.paas.wf.WorkflowsBuilderService.TopologyContext;
import alien4cloud.paas.wf.util.WorkflowUtils;
import lombok.extern.slf4j.Slf4j;

@Component("cloudify-deployment-builder-service")
@Slf4j
public class CloudifyDeploymentBuilderService {
    @Inject
    private WorkflowsBuilderService workflowBuilderService;
    @Inject
    private OrchestratorDeploymentPropertiesService deploymentPropertiesService;
    @Inject
    private CfyConnectionManager connectionManager;
    @Inject
    @Lazy(true)
    private ILocationResourceService locationResourceService;
    /** Service that force the presence of a create operation to trigger initializations. */
    @Inject
    private InitOperationInjectorService initOperationInjectorService;
    @Inject
    private ServiceDelegateWorkflowService serviceDelegateWorkflowService;

    /**
     * Build the Cloudify deployment from the deployment context. Cloudify deployment has data pre-parsed so that blueprint generation is easier.
     *
     * @param deploymentContext the deployment context
     * @return the cloudify deployment
     */
    public CloudifyDeployment buildCloudifyDeployment(PaaSTopologyDeploymentContext deploymentContext) {
        CloudifyDeployment cloudifyDeployment = new CloudifyDeployment();

        processNetworks(cloudifyDeployment, deploymentContext);
        processDeploymentArtifacts(cloudifyDeployment, deploymentContext);

        // this is a set of all node type provided by all the locations this deployment is related on
        Set<String> locationProvidedTypes = Sets.newHashSet();
        for (Location location : deploymentContext.getLocations().values()) {
            LocationResources locationResources = locationResourceService.getLocationResources(location);
            locationProvidedTypes.addAll(locationResources.getProvidedTypes());
        }

        List<NodeType> nativeTypes = getTypesOrderedByDerivedFromHierarchy(
                excludeCustomNativeTypes(deploymentContext.getPaaSTopology().getComputes(), locationProvidedTypes));
        nativeTypes.addAll(getTypesOrderedByDerivedFromHierarchy(deploymentContext.getPaaSTopology().getNetworks()));
        nativeTypes.addAll(getTypesOrderedByDerivedFromHierarchy(deploymentContext.getPaaSTopology().getVolumes()));

        cloudifyDeployment.setDeploymentPaaSId(deploymentContext.getDeploymentPaaSId());
        cloudifyDeployment.setDeploymentId(deploymentContext.getDeploymentId());
        cloudifyDeployment.setLocationType(getLocationType(deploymentContext));
        cloudifyDeployment.setComputes(excludeCustomNativeTypes(deploymentContext.getPaaSTopology().getComputes(), locationProvidedTypes));
        cloudifyDeployment.setVolumes(deploymentContext.getPaaSTopology().getVolumes());
        cloudifyDeployment.setNonNatives(deploymentContext.getPaaSTopology().getNonNatives());

        // Filter docker types
        List<PaaSNodeTemplate> dockerTypes = extractDockerType(deploymentContext.getPaaSTopology().getNonNatives());
        cloudifyDeployment.setDockerTypes(dockerTypes);
        cloudifyDeployment.getNonNatives().removeAll(dockerTypes); // TODO Not needed thanks to custom resources filtering ?

        // Filter custom resources
        Map<String, PaaSNodeTemplate> customResources = extractCustomNativeType(deploymentContext.getPaaSTopology().getAllNodes(), locationProvidedTypes);
        cloudifyDeployment.setCustomResources(customResources);
        Collection<PaaSNodeTemplate> customNatives = customResources.values();
        if (!customNatives.isEmpty()) {
            if (cloudifyDeployment.getNonNatives() == null) {
                List<PaaSNodeTemplate> nonNatives = Lists.newArrayList(customNatives);
                cloudifyDeployment.setNonNatives(nonNatives);
            } else {
                cloudifyDeployment.getNonNatives().addAll(customNatives);
            }
        }

        processNonNativeTypes(cloudifyDeployment, cloudifyDeployment.getNonNatives());
        cloudifyDeployment.setNativeTypes(nativeTypes);

        cloudifyDeployment.setAllNodes(deploymentContext.getPaaSTopology().getAllNodes());
        cloudifyDeployment.setProviderDeploymentProperties(deploymentContext.getDeploymentTopology().getProviderDeploymentProperties());

        // Replace services delegate operation with create and start operations
        replaceServiceDelegate(cloudifyDeployment.getNonNatives(), deploymentContext.getDeploymentTopology().getWorkflows());
        // Cloudify plugin injects a specific operation required for nodes initializations.
        injectInitOperations(cloudifyDeployment.getNonNatives(), deploymentContext.getDeploymentTopology().getWorkflows());

        cloudifyDeployment.setWorkflows(buildWorkflowsForDeployment(deploymentContext));

        // if monitoring is enabled then try to get the nodes to monitor
        setNodesToMonitor(cloudifyDeployment);

        // load the mappings for the native types.
        TopologyContext topologyContext = workflowBuilderService.buildTopologyContext(deploymentContext.getDeploymentTopology());
        cloudifyDeployment.setPropertyMappings(PropertiesMappingUtil.loadPropertyMappings(cloudifyDeployment.getNativeTypes(), topologyContext));

        cloudifyDeployment.setCapabilityTypes(deploymentContext.getPaaSTopology().getCapabilityTypes());

        return cloudifyDeployment;
    }

    private List<PaaSNodeTemplate> extractDockerType(List<PaaSNodeTemplate> nodes) {
        List<PaaSNodeTemplate> result = Lists.newArrayList();
        for (PaaSNodeTemplate value : nodes) {
            if (ToscaNormativeUtil.isFromType(BlueprintService.TOSCA_DOCKER_CONTAINER_TYPE, value.getIndexedToscaElement())) {
                result.add(value);
            }
        }
        return result;
    }

    private Map<String, PaaSNodeTemplate> extractCustomNativeType(Map<String, PaaSNodeTemplate> nodes, Set<String> locationProvidedTypes) {
        Map<String, PaaSNodeTemplate> result = Maps.newHashMap();
        for (Map.Entry<String, PaaSNodeTemplate> entry : nodes.entrySet()) {
            if (isCustomResource(entry.getValue(), locationProvidedTypes)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * A custom resource is a template that:
     * <ul>
     * <li>is not of a type provided by the location</li>
     * <li>AND doesn't have a host</li>
     * *
     * <li>AND is not a Docker container</li>
     * *
     * <li>AND is not a service</li>
     * </ul>
     *
     * @param node
     * @return true is the node is considered as a custom template.
     */
    private boolean isCustomResource(PaaSNodeTemplate node, Set<String> locationProvidedTypes) {
        if (node.getParent() != null) {
            // if the node template has a parent, it can't be considered as a custom resource.
            return false;
        }
        if (locationProvidedTypes.contains(node.getTemplate().getType())) {
            // if the type of the template is provided by the location, it can't be considered as a custom resource.
            return false;
        }
        if (ToscaNormativeUtil.isFromType(BlueprintService.TOSCA_DOCKER_CONTAINER_TYPE, node.getIndexedToscaElement())) {
            // Docker Container types are not custom resources
            return false;
        }
        if (node.getTemplate() instanceof ServiceNodeTemplate) {
            // a service has no host and it's type is not provided by the location but is not a custom resource
            return false;
        }
        return true;
    }

    private List<PaaSNodeTemplate> excludeCustomNativeTypes(Collection<PaaSNodeTemplate> nodes, Set<String> locationProvidedTypes) {
        List<PaaSNodeTemplate> result = Lists.newArrayList();
        for (PaaSNodeTemplate node : nodes) {
            if (!isCustomResource(node, locationProvidedTypes)) {
                result.add(node);
            }
        }
        return result;
    }

    private void setNodesToMonitor(CloudifyDeployment cloudifyDeployment) {
        String autoHeal = deploymentPropertiesService.getValueOrDefault(cloudifyDeployment.getProviderDeploymentProperties(),
                DeploymentPropertiesNames.AUTO_HEAL);
        boolean isDisableDiamond = connectionManager.getConfiguration().getDisableDiamondMonitorAgent() == null ? false
                : connectionManager.getConfiguration().getDisableDiamondMonitorAgent();
        if (Boolean.parseBoolean(autoHeal) && !isDisableDiamond) {
            cloudifyDeployment.setNodesToMonitor(getNodesToMonitor(cloudifyDeployment.getComputes()));
        }
    }

    public Workflows buildWorkflowsForDeployment(PaaSTopologyDeploymentContext deploymentContext) {
        Map<String, Workflow> workflowsMap = deploymentContext.getDeploymentTopology().getWorkflows();
        WorkflowUtils.processInlineWorkflows(workflowsMap);
        Workflows workflows = new Workflows();
        workflows.setWorkflows(workflowsMap);
        fillWorkflowSteps(INSTALL, workflowsMap, workflows.getInstallHostWorkflows());
        fillWorkflowSteps(UNINSTALL, workflowsMap, workflows.getUninstallHostWorkflows());
        fillOrphans(INSTALL, workflowsMap, workflows.getStandardWorkflows());
        fillOrphans(UNINSTALL, workflowsMap, workflows.getStandardWorkflows());
        return workflows;
    }

    /**
     * We use the create operation using a post create code injection to set all node properties as attributes, set the ip attribute on the endpoint
     * capabilities and set eventually attribute values if some are defined. Inject create operations used for node initialization in node that do not
     * define them. Also inject call to create operation before the node created
     * state change.
     *
     * @param nonNatives
     * @param workflowsMap
     */
    private void injectInitOperations(List<PaaSNodeTemplate> nonNatives, Map<String, Workflow> workflowsMap) {
        Workflow installWorkflow = workflowsMap.get(INSTALL);
        // for every non native node let's ensure that there is a call to the create method.
        for (PaaSNodeTemplate template : nonNatives) {
            initOperationInjectorService.ensureCreateOperation(template, installWorkflow);
        }
    }

    /**
     * Replace the service delegate operations by a create and start operation call sequence.
     *
     * @param nonNatives Services nodes are stored as non native nodes.
     * @param workflowsMap The workflow map to impact.
     */
    private void replaceServiceDelegate(List<PaaSNodeTemplate> nonNatives, Map<String, Workflow> workflowsMap) {
        for (PaaSNodeTemplate template : nonNatives) {
            if (template.getTemplate() instanceof ServiceNodeTemplate) {
                // Service node must not have any operations at the node level
                // The node is just a technical trick to implement service in Alien
                if (template.getTemplate().getInterfaces() != null) {
                    template.getTemplate().getInterfaces().clear();
                }
                if (template.getInterfaces() != null) {
                    template.getInterfaces().clear();
                }
                serviceDelegateWorkflowService.replaceInstallServiceDelegate(template, workflowsMap.get(INSTALL));
                serviceDelegateWorkflowService.replaceUnInstallServiceDelegate(template, workflowsMap.get(UNINSTALL));
            }
        }
    }

    private void fillOrphans(String workflowName, Map<String, Workflow> workflows, Map<String, StandardWorkflow> standardWorkflows) {
        Workflow workflow = workflows.get(workflowName);
        StandardWorkflow standardWorkflow = buildOrphansWorkflow(workflow);
        standardWorkflows.put(workflowName, standardWorkflow);
    }

    private StandardWorkflow buildOrphansWorkflow(Workflow workflow) {
        StandardWorkflow standardWorkflow = new StandardWorkflow();
        standardWorkflow.setHosts(workflow.getHosts());
        standardWorkflow.setOrphanSteps(getOrphanSteps(workflow));
        standardWorkflow.setLinks(buildFollowingLinksFromSteps(standardWorkflow.getOrphanSteps()));
        return standardWorkflow;
    }

    /**
     * get steps without parent, i-e without hostId
     *
     * @param workflow
     * @return
     */
    private Map<String, WorkflowStep> getOrphanSteps(Workflow workflow) {
        return getHostRelatedSteps(null, workflow);
    }

    private void fillWorkflowSteps(String workflowName, Map<String, Workflow> workflows, Map<String, HostWorkflow> workflowSteps) {
        Workflow workflow = workflows.get(workflowName);
        Set<String> hostIds = workflow.getHosts();
        if (CollectionUtils.isEmpty(hostIds)) {
            return;
        }
        for (String hostId : hostIds) {
            HostWorkflow hostWorkflow = buildHostWorkflow(hostId, workflow);
            workflowSteps.put(hostId, hostWorkflow);
        }
    }

    private HostWorkflow buildHostWorkflow(String hostId, Workflow workflow) {
        HostWorkflow hostWorkflow = new HostWorkflow();
        hostWorkflow.setSteps(getHostRelatedSteps(hostId, workflow));
        hostWorkflow.getSteps().putAll(getOrphanRelatedSteps(hostWorkflow.getSteps(), getOrphanSteps(workflow)));
        processLinks(hostWorkflow.getSteps(), hostWorkflow.getInternalLinks(), hostWorkflow.getExternalLinks());
        // hostWorkflow.setInternalLinks(getLinksBetweenSteps(hostWorkflow.getSteps()));
        return hostWorkflow;
    }

    /**
     * Get orphan steps that are related to the workflow.
     *
     * @param workflow The given workflow
     * @param orphanSteps Map of orphan steps
     * @return A Map of orphans that are related to the given workflow.
     */
    private Map<String, WorkflowStep> getOrphanRelatedSteps(Map<String, WorkflowStep> workflow, Map<String, WorkflowStep> orphanSteps) {
        Map<String, WorkflowStep> relatedSteps = Maps.newLinkedHashMap();
        for (WorkflowStep step : orphanSteps.values()) {
            for (WorkflowStep sh : workflow.values()) {
                if ((sh.getPrecedingSteps() != null && sh.getPrecedingSteps().contains(step.getName()))
                        || (sh.getOnSuccess() != null && sh.getOnSuccess().contains(step.getName()))) {
                    relatedSteps.put(step.getName(), step);
                }
            }
        }
        return relatedSteps;
    }

    private void processLinks(Map<String, WorkflowStep> steps, List<WorkflowStepLink> internalLinks, List<WorkflowStepLink> externalLinks) {
        for (WorkflowStep step : steps.values()) {
            buildFollowingLinksFromStep(step, steps, internalLinks, externalLinks);
        }
    }

    /**
     * build a list of {@link WorkflowStepLink} between a given map of steps
     *
     * @param steps
     * @return
     */
    private List<WorkflowStepLink> buildFollowingLinksFromSteps(Map<String, WorkflowStep> steps) {
        List<WorkflowStepLink> links = Lists.newArrayList();
        for (WorkflowStep step : steps.values()) {
            links.addAll(buildFollowingLinksFromStep(step));
        }
        return links;
    }

    private List<WorkflowStepLink> buildFollowingLinksFromStep(WorkflowStep step) {
        List<WorkflowStepLink> links = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(step.getOnSuccess())) {
            for (String following : step.getOnSuccess()) {
                links.add(new WorkflowStepLink(step.getName(), following));
            }
        }
        return links;
    }

    /**
     * build lists of {@link WorkflowStepLink} representing a link from a given step to others steps.
     * The links can be internal to a set of given steps, or externals
     *
     * @param step step from which to get the links
     * @param steps The map of steps in which the following step related to the internal link should be
     * @param externalLinks
     * @param internalLinks
     * @return
     */
    private void buildFollowingLinksFromStep(WorkflowStep step, Map<String, WorkflowStep> steps, List<WorkflowStepLink> internalLinks,
            List<WorkflowStepLink> externalLinks) {
        List<WorkflowStepLink> links = buildFollowingLinksFromStep(step);
        for (WorkflowStepLink link : links) {
            WorkflowStep following = steps.get(link.getToStepId());
            if (following != null) {
                internalLinks.add(link);
            } else {
                externalLinks.add(link);
            }
        }
    }

    /**
     * Get steps related to a specific hostId
     *
     * @param hostId
     * @param workflow
     * @return
     */
    private Map<String, WorkflowStep> getHostRelatedSteps(String hostId, Workflow workflow) {
        Map<String, WorkflowStep> steps = Maps.newLinkedHashMap();
        for (WorkflowStep step : workflow.getSteps().values()) {
            if (step instanceof NodeWorkflowStep) {
                if (isNodeStepRelatedToHost((NodeWorkflowStep) step, hostId)) {
                    // Only step related to the host is added
                    steps.put(step.getName(), step);
                }
            } else {
                if (isRelationshipStepRelatedToHost((RelationshipWorkflowStep) step, hostId)) {
                    // The source of relationship is on the host
                    steps.put(step.getName(), step);
                }
            }
        }
        return steps;
    }

    private boolean isNodeStepRelatedToHost(NodeWorkflowStep step, String hostId) {
        return Objects.equals(step.getHostId(), hostId);
    }

    private boolean isRelationshipStepRelatedToHost(RelationshipWorkflowStep step, String hostId) {
        return Objects.equals(step.getSourceHostId(), hostId) || Objects.equals(step.getTargetHostId(), hostId);
    }

    private List<NodeType> getTypesOrderedByDerivedFromHierarchy(List<PaaSNodeTemplate> nodes) {
        Map<String, NodeType> nodeTypeMap = Maps.newLinkedHashMap();
        for (PaaSNodeTemplate node : nodes) {
            nodeTypeMap.put(node.getIndexedToscaElement().getElementId(), node.getIndexedToscaElement());
        }
        return IndexedModelUtils.orderByDerivedFromHierarchy(nodeTypeMap);
    }

    /**
     * Get the location of the deployment from the context.
     */
    private String getLocationType(PaaSTopologyDeploymentContext deploymentContext) {
        if (MapUtils.isEmpty(deploymentContext.getLocations()) || deploymentContext.getLocations().size() != 1) {
            throw new SingleLocationRequiredException();
        }
        return deploymentContext.getLocations().values().iterator().next().getInfrastructureType();
    }

    /**
     * Map the networks from the topology to either public or private network.
     * Cloudify plugin indeed maps the public network to floating ips while private network are mapped to network and subnets.
     *
     * @param cloudifyDeployment The cloudify deployment context with private and public networks mapped.
     * @param deploymentContext The deployment context from alien 4 cloud.
     */
    private void processNetworks(CloudifyDeployment cloudifyDeployment, PaaSTopologyDeploymentContext deploymentContext) {
        List<PaaSNodeTemplate> allNetworks = deploymentContext.getPaaSTopology().getNetworks();
        List<PaaSNodeTemplate> publicNetworks = Lists.newArrayList();
        List<PaaSNodeTemplate> privateNetworks = Lists.newArrayList();
        for (PaaSNodeTemplate network : allNetworks) {
            if (ToscaNormativeUtil.isFromType("alien.nodes.PublicNetwork", network.getIndexedToscaElement())) {
                publicNetworks.add(network);
            } else if (ToscaNormativeUtil.isFromType("alien.nodes.PrivateNetwork", network.getIndexedToscaElement())) {
                privateNetworks.add(network);
            } else {
                throw new InvalidArgumentException(
                        "The type " + network.getTemplate().getType() + " must extends alien.nodes.PublicNetwork or alien.nodes.PrivateNetwork");
            }
        }

        cloudifyDeployment.setExternalNetworks(publicNetworks);
        cloudifyDeployment.setInternalNetworks(privateNetworks);
    }

    /**
     * Extract the types of all types that are not provided by cloudify (non-native types) for both nodes and relationships.
     * Types have to be generated in the blueprint in correct order (based on derived from hierarchy).
     *
     * @param cloudifyDeployment The cloudify deployment context with private and public networks mapped.
     * @param nonNativeNodes The list of non native types.
     */
    private void processNonNativeTypes(CloudifyDeployment cloudifyDeployment, List<PaaSNodeTemplate> nonNativeNodes) {
        Map<String, NodeType> nonNativesTypesMap = Maps.newLinkedHashMap();
        Map<String, RelationshipType> nonNativesRelationshipsTypesMap = Maps.newLinkedHashMap();
        if (nonNativeNodes != null) {
            for (PaaSNodeTemplate nonNative : nonNativeNodes) {
                nonNativesTypesMap.put(nonNative.getIndexedToscaElement().getElementId(), nonNative.getIndexedToscaElement());
                List<PaaSRelationshipTemplate> relationshipTemplates = nonNative.getRelationshipTemplates();
                for (PaaSRelationshipTemplate relationshipTemplate : relationshipTemplates) {
                    if (!NormativeRelationshipConstants.DEPENDS_ON.equals(relationshipTemplate.getIndexedToscaElement().getElementId())
                            && !NormativeRelationshipConstants.HOSTED_ON.equals(relationshipTemplate.getIndexedToscaElement().getElementId())) {
                        nonNativesRelationshipsTypesMap.put(relationshipTemplate.getIndexedToscaElement().getElementId(),
                                relationshipTemplate.getIndexedToscaElement());
                    }
                }
            }
        }

        cloudifyDeployment.setNonNativesTypes(IndexedModelUtils.orderByDerivedFromHierarchy(nonNativesTypesMap));
        cloudifyDeployment.setNonNativesRelationshipTypes(IndexedModelUtils.orderByDerivedFromHierarchy(nonNativesRelationshipsTypesMap));
    }

    private void processDeploymentArtifacts(CloudifyDeployment cloudifyDeployment, PaaSTopologyDeploymentContext deploymentContext) {
        Map<String, Map<String, DeploymentArtifact>> allArtifacts = Maps.newLinkedHashMap();
        Map<Relationship, Map<String, DeploymentArtifact>> allRelationshipArtifacts = Maps.newLinkedHashMap();
        for (Map.Entry<String, PaaSNodeTemplate> nodeEntry : deploymentContext.getPaaSTopology().getAllNodes().entrySet()) {
            PaaSNodeTemplate node = nodeEntry.getValue();
            // add the node artifacts
            putArtifacts(allArtifacts, node.getId(), node.getTemplate().getArtifacts());
            // add the relationships artifacts
            addRelationshipsArtifacts(allRelationshipArtifacts, nodeEntry.getValue().getRelationshipTemplates());
        }

        cloudifyDeployment.setAllDeploymentArtifacts(allArtifacts);
        cloudifyDeployment.setAllRelationshipDeploymentArtifacts(allRelationshipArtifacts);
    }

    private void addRelationshipsArtifacts(Map<Relationship, Map<String, DeploymentArtifact>> allRelationshipArtifacts,
            List<PaaSRelationshipTemplate> relationships) {
        if (relationships == null || relationships.isEmpty()) {
            return;
        }

        for (PaaSRelationshipTemplate relationship : relationships) {
            Map<String, DeploymentArtifact> artifacts = relationship.getTemplate().getArtifacts();

            putArtifacts(allRelationshipArtifacts, new Relationship(relationship.getId(), relationship.getSource(), relationship.getTemplate().getTarget()),
                    artifacts);
        }
    }

    private <T> void putArtifacts(Map<T, Map<String, DeploymentArtifact>> targetMap, T key, Map<String, DeploymentArtifact> artifacts) {
        if (artifacts != null && !artifacts.isEmpty()) {
            targetMap.put(key, artifacts);
        }
    }

    private Set<PaaSNodeTemplate> getNodesToMonitor(List<PaaSNodeTemplate> computes) {
        Set<PaaSNodeTemplate> nodesToMonitor = Sets.newLinkedHashSet();
        for (PaaSNodeTemplate compute : computes) {
            // we monitor only if the compute is not a windows type
            // TODO better way to find that this is not a windows compute, taking in accound the location
            if (!compute.getIndexedToscaElement().getElementId().contains("WindowsCompute")) {
                nodesToMonitor.add(compute);
            }
        }
        return nodesToMonitor;
    }
}
