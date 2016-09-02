package alien4cloud.paas.cloudify3.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.components.IndexedModelUtils;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
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
import alien4cloud.paas.wf.AbstractStep;
import alien4cloud.paas.wf.NodeActivityStep;
import alien4cloud.paas.wf.Workflow;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.paas.wf.WorkflowsBuilderService.TopologyContext;
import alien4cloud.tosca.ToscaUtils;
import alien4cloud.tosca.normative.NormativeRelationshipConstants;
import lombok.extern.slf4j.Slf4j;

@Component("cloudify-deployment-builder-service")
@Slf4j
public class CloudifyDeploymentBuilderService {

    @Inject
    private WorkflowsBuilderService workflowBuilderService;
    @Inject
    private OrchestratorDeploymentPropertiesService deploymentPropertiesService;

    /**
     * Build the Cloudify deployment from the deployment context. Cloudify deployment has data pre-parsed so that blueprint generation is easier.
     *
     * @param deploymentContext
     *            the deployment context
     * @return the cloudify deployment
     */
    public CloudifyDeployment buildCloudifyDeployment(PaaSTopologyDeploymentContext deploymentContext) {
        CloudifyDeployment cloudifyDeployment = new CloudifyDeployment();

        processNetworks(cloudifyDeployment, deploymentContext);
        processNonNativeTypes(cloudifyDeployment, deploymentContext);
        processDeploymentArtifacts(cloudifyDeployment, deploymentContext);

        List<IndexedNodeType> nativeTypes = getTypesOrderedByDerivedFromHierarchy(deploymentContext.getPaaSTopology().getComputes());
        nativeTypes.addAll(getTypesOrderedByDerivedFromHierarchy(deploymentContext.getPaaSTopology().getNetworks()));
        nativeTypes.addAll(getTypesOrderedByDerivedFromHierarchy(deploymentContext.getPaaSTopology().getVolumes()));

        cloudifyDeployment.setDeploymentPaaSId(deploymentContext.getDeploymentPaaSId());
        cloudifyDeployment.setDeploymentId(deploymentContext.getDeploymentId());
        cloudifyDeployment.setLocationType(getLocationType(deploymentContext));
        cloudifyDeployment.setComputes(deploymentContext.getPaaSTopology().getComputes());
        cloudifyDeployment.setVolumes(deploymentContext.getPaaSTopology().getVolumes());
        cloudifyDeployment.setNonNatives(deploymentContext.getPaaSTopology().getNonNatives());
        cloudifyDeployment.setNativeTypes(nativeTypes);

        cloudifyDeployment.setAllNodes(deploymentContext.getPaaSTopology().getAllNodes());
        cloudifyDeployment.setProviderDeploymentProperties(deploymentContext.getDeploymentTopology().getProviderDeploymentProperties());
        cloudifyDeployment.setWorkflows(buildWorkflowsForDeployment(deploymentContext.getDeploymentTopology().getWorkflows()));

        // if monitoring is enabled then try to get the nodes to monitor
        setNodesToMonitor(cloudifyDeployment);

        // load the mappings for the native types.
        TopologyContext topologyContext = workflowBuilderService.buildTopologyContext(deploymentContext.getDeploymentTopology());
        cloudifyDeployment.setPropertyMappings(PropertiesMappingUtil.loadPropertyMappings(cloudifyDeployment.getNativeTypes(), topologyContext));

        return cloudifyDeployment;
    }

    private void setNodesToMonitor(CloudifyDeployment cloudifyDeployment) {
        String autoHeal = deploymentPropertiesService.getValueOrDefault(cloudifyDeployment.getProviderDeploymentProperties(),
                DeploymentPropertiesNames.AUTO_HEAL);
        if (Boolean.parseBoolean(autoHeal)) {
            cloudifyDeployment.setNodesToMonitor(getNodesToMonitor(cloudifyDeployment.getComputes()));
        }
    }

    // TODO: shouldn't we put this in utils intead??
    public Workflows buildWorkflowsForDeployment(Map<String, Workflow> workflowsMap) {
        Workflows workflows = new Workflows();
        workflows.setWorkflows(workflowsMap);
        fillWorkflowSteps(Workflow.INSTALL_WF, workflowsMap, workflows.getInstallHostWorkflows());
        fillWorkflowSteps(Workflow.UNINSTALL_WF, workflowsMap, workflows.getUninstallHostWorkflows());
        fillOrphans(Workflow.INSTALL_WF, workflowsMap, workflows.getStandardWorkflows());
        fillOrphans(Workflow.UNINSTALL_WF, workflowsMap, workflows.getStandardWorkflows());
        return workflows;
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
    private Map<String, AbstractStep> getOrphanSteps(Workflow workflow) {
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
    private Map<String, AbstractStep> getOrphanRelatedSteps(Map<String, AbstractStep> workflow, Map<String, AbstractStep> orphanSteps) {
        Map<String, AbstractStep> relatedSteps = Maps.newLinkedHashMap();
        for (AbstractStep step : orphanSteps.values()) {
            if (step instanceof NodeActivityStep) {
                for (AbstractStep sh : workflow.values()) {
                    if ((sh.getPrecedingSteps() != null && sh.getPrecedingSteps().contains(step.getName())) || (sh.getFollowingSteps() != null && sh
                            .getFollowingSteps().contains(step.getName()))) {
                        relatedSteps.put(step.getName(), step);
                    }
                }
            }
        }
        return relatedSteps;
    }

    private void processLinks(Map<String, AbstractStep> steps, List<WorkflowStepLink> internalLinks, List<WorkflowStepLink> externalLinks) {
        for (AbstractStep step : steps.values()) {
            buildFollowingLinksFromStep(step, steps, internalLinks, externalLinks);
        }
    }

    /**
     * build a list of {@link WorkflowStepLink} between a given map of steps
     *
     * @param steps
     * @return
     */
    private List<WorkflowStepLink> buildFollowingLinksFromSteps(Map<String, AbstractStep> steps) {
        List<WorkflowStepLink> links = Lists.newArrayList();
        for (AbstractStep step : steps.values()) {
            links.addAll(buildFollowingLinksFromStep(step));
        }
        return links;
    }

    private List<WorkflowStepLink> buildFollowingLinksFromStep(AbstractStep step) {
        List<WorkflowStepLink> links = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(step.getFollowingSteps())) {
            for (String following : step.getFollowingSteps()) {
                links.add(new WorkflowStepLink(step.getName(), following));
            }
        }
        return links;
    }

    /**
     * build lists of {@link WorkflowStepLink} representing a link from a given step to others steps.
     * The links can be internal to a set of given steps, or externals
     *
     * @param step          step from which to get the links
     * @param steps         The map of steps in which the following step related to the internal link should be
     * @param externalLinks
     * @param internalLinks
     * @return
     */
    private void buildFollowingLinksFromStep(AbstractStep step, Map<String, AbstractStep> steps, List<WorkflowStepLink> internalLinks,
            List<WorkflowStepLink> externalLinks) {
        List<WorkflowStepLink> links = buildFollowingLinksFromStep(step);
        for (WorkflowStepLink link : links) {
            AbstractStep following = steps.get(link.getToStepId());
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
    private Map<String, AbstractStep> getHostRelatedSteps(String hostId, Workflow workflow) {
        Map<String, AbstractStep> steps = Maps.newLinkedHashMap();
        for (AbstractStep step : workflow.getSteps().values()) {
            // proceed only NodeActivityStep
            if (step instanceof NodeActivityStep) {
                if (isStepRelatedToHost((NodeActivityStep) step, hostId)) {
                    steps.put(step.getName(), step);
                }
            }
        }
        return steps;
    }

    private boolean isStepRelatedToHost(NodeActivityStep step, String hostId) {
        return Objects.equals(step.getHostId(), hostId);
    }

    private List<IndexedNodeType> getTypesOrderedByDerivedFromHierarchy(List<PaaSNodeTemplate> nodes) {
        Map<String, IndexedNodeType> nodeTypeMap = Maps.newLinkedHashMap();
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
     * Cloudify 3 plugin indeed maps the public network to floating ips while private network are mapped to network and subnets.
     *
     * @param cloudifyDeployment The cloudify deployment context with private and public networks mapped.
     * @param deploymentContext  The deployment context from alien 4 cloud.
     */
    private void processNetworks(CloudifyDeployment cloudifyDeployment, PaaSTopologyDeploymentContext deploymentContext) {
        List<PaaSNodeTemplate> allNetworks = deploymentContext.getPaaSTopology().getNetworks();
        List<PaaSNodeTemplate> publicNetworks = Lists.newArrayList();
        List<PaaSNodeTemplate> privateNetworks = Lists.newArrayList();
        for (PaaSNodeTemplate network : allNetworks) {
            if (ToscaUtils.isFromType("alien.nodes.PublicNetwork", network.getIndexedToscaElement())) {
                publicNetworks.add(network);
            } else if (ToscaUtils.isFromType("alien.nodes.PrivateNetwork", network.getIndexedToscaElement())) {
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
     * @param deploymentContext  The deployment context from alien 4 cloud.
     */
    private void processNonNativeTypes(CloudifyDeployment cloudifyDeployment, PaaSTopologyDeploymentContext deploymentContext) {
        Map<String, IndexedNodeType> nonNativesTypesMap = Maps.newLinkedHashMap();
        Map<String, IndexedRelationshipType> nonNativesRelationshipsTypesMap = Maps.newLinkedHashMap();
        for (PaaSNodeTemplate nonNative : deploymentContext.getPaaSTopology().getNonNatives()) {
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

        cloudifyDeployment.setNonNativesTypes(IndexedModelUtils.orderByDerivedFromHierarchy(nonNativesTypesMap));
        cloudifyDeployment.setNonNativesRelationshipTypes(IndexedModelUtils.orderByDerivedFromHierarchy(nonNativesRelationshipsTypesMap));
    }

    private void processDeploymentArtifacts(CloudifyDeployment cloudifyDeployment, PaaSTopologyDeploymentContext deploymentContext) {
        Map<String, Map<String, DeploymentArtifact>> allArtifacts = Maps.newLinkedHashMap();
        Map<Relationship, Map<String, DeploymentArtifact>> allRelationshipArtifacts = Maps.newLinkedHashMap();
        for (Map.Entry<String, PaaSNodeTemplate> nodeEntry : deploymentContext.getPaaSTopology().getAllNodes().entrySet()) {
            PaaSNodeTemplate node = nodeEntry.getValue();
            // add the node artifacts
            putArtifacts(allArtifacts, node.getId(), node.getIndexedToscaElement().getArtifacts());
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
            Map<String, DeploymentArtifact> artifacts = relationship.getIndexedToscaElement().getArtifacts();

            putArtifacts(allRelationshipArtifacts,
                    new Relationship(relationship.getId(), relationship.getSource(), relationship.getRelationshipTemplate().getTarget()), artifacts);
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
