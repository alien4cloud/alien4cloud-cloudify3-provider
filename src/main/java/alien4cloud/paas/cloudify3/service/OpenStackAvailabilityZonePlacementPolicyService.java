package alien4cloud.paas.cloudify3.service;

import java.util.*;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import org.alien4cloud.tosca.model.templates.AbstractPolicy;
import org.alien4cloud.tosca.model.templates.HaPolicy;
import org.alien4cloud.tosca.model.templates.NodeGroup;
import alien4cloud.orchestrators.locations.services.ILocationResourceService;
import alien4cloud.orchestrators.plugin.ILocationResourceAccessor;
import alien4cloud.paas.cloudify3.error.AZAssignmentException;
import alien4cloud.paas.model.PaaSInstancePersistentResourceMonitorEvent;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;

/**
 * Workaround to enable back ha policy support in openstack.
 */
@Slf4j
@Component
public class OpenStackAvailabilityZonePlacementPolicyService {
    public static final String SERVER_PROPERTY = "server";
    public static final String VOLUME_PROPERTY = "volume";
    public static final String AZ_KEY = "availability_zone";

    @Inject
    @Lazy(true)
    private ILocationResourceService locationResourceService;

    @Inject
    private EventService eventService;


    /**
     * Pre-process the topology to add availability zones based on the defined H.A. policies.
     *
     * @param deploymentContext The deployment context that contains the topology to update.
     */
    public void process(PaaSTopologyDeploymentContext deploymentContext) {
        Location location = deploymentContext.getLocations().values().iterator().next();

        if (!"openstack".equals(location.getInfrastructureType())) {
            log.debug("H.A. policy is supported only for openstack.");
            return;
        }

        if (deploymentContext.getDeploymentTopology().getGroups() == null) {
            return;
        }

        // for every group defined
        for (NodeGroup nodeGroup : deploymentContext.getDeploymentTopology().getGroups().values()) {
            // process policy
            for (AbstractPolicy policy : nodeGroup.getPolicies()) {
                processPolicy(location, deploymentContext, policy, nodeGroup.getName(), nodeGroup.getMembers());
            }
        }
    }

    private void processPolicy(Location location, PaaSTopologyDeploymentContext deploymentContext, AbstractPolicy policy, String groupName, Set<String> members) {
        if (!(policy instanceof HaPolicy)) {
            // we skip all policies but h.a.
            log.debug("Skipping policy as type is not h.a. but {}", policy.getType());
            return;
        }

        ILocationResourceAccessor accessor = locationResourceService.accessor(location.getId());
        List<LocationResourceTemplate> availabilityZones = accessor.getResources("alien.cloudify.openstack.nodes.AvailabilityZone");
        if (availabilityZones == null || availabilityZones.size() < 2) {
            log.warn("Skipping policy as administrator has not defined at least 2 availability zones.");
            return;
        }

        setAZForAllNodes(deploymentContext.getDeploymentId(), deploymentContext.getPaaSTopology().getAllNodes(), availabilityZones, groupName, members);
    }

    public void setAZForAllNodes(String deploymentId, Map<String, PaaSNodeTemplate> allNodes, List<LocationResourceTemplate> availabilityZones, String groupName, Set<String> members) {
        LinkedHashSet<String> membersSorted = sortMembersByVolumeAndAZ(allNodes, members);
        Map<String, Integer> countUsageOfAZOnServer = initMapOfUsedAZ(availabilityZones);

        for (String member : membersSorted) {
            String availabilityZone = null;
            PaaSNodeTemplate target = allNodes.get(member);

            if (target.getStorageNodes() != null) {
                // there is storage nodes, try to put them in same AZ
                for (PaaSNodeTemplate volumeNode : target.getStorageNodes()) {
                    String storageAZ = getAZ(volumeNode, VOLUME_PROPERTY);
                    if (availabilityZone == null) {
                        availabilityZone = storageAZ;
                    } else if (!availabilityZone.equals(storageAZ)) {
                        log.error("Volumes attached to node {} are defined in different availability zones. HA policy will not be applied for group {}.",
                                target.getId(), groupName);
                        throw new AZAssignmentException("Volumes attached to node cannot lie in different availability zones.");
                    }
                }
            }

            if (availabilityZone == null) {
                availabilityZone = getLessUsedAZ(countUsageOfAZOnServer);
            }

            // set the availability zone to all nodes
            setAZ(deploymentId, target, availabilityZone, SERVER_PROPERTY);
            countUsageOfAZOnServer.put(availabilityZone, countUsageOfAZOnServer.get(availabilityZone) + 1);
            if (target.getStorageNodes() != null) {
                for (PaaSNodeTemplate volumeNode : target.getStorageNodes()) {
                    setAZ(deploymentId, volumeNode, availabilityZone, VOLUME_PROPERTY);
                }
            }
        }
    }

    private String getAZ(PaaSNodeTemplate paaSNodeTemplate, String handlerPropertyKey) {
        AbstractPropertyValue volumePropertyValue = paaSNodeTemplate.getTemplate().getProperties().get(handlerPropertyKey);
        if (volumePropertyValue != null && volumePropertyValue instanceof PropertyValue) {
            Object volume = ((PropertyValue) volumePropertyValue).getValue();
            if (volume instanceof Map) {
                return (String) ((Map) volume).get(AZ_KEY);
            }
        }
        return null;
    }

    private void setAZ(String deploymentId, PaaSNodeTemplate paaSNodeTemplate, String availabilityZone, String handlerPropertyKey) {
        // Enqueue events so the fields will be updated in the deployment topology
        AbstractPropertyValue volumePropertyValue = paaSNodeTemplate.getTemplate().getProperties().get(handlerPropertyKey);
        if (volumePropertyValue == null) {
            volumePropertyValue = new ComplexPropertyValue();
            ((PropertyValue<Map<String, Object>>) volumePropertyValue).setValue(new HashMap<String, Object>());
            paaSNodeTemplate.getTemplate().getProperties().put(handlerPropertyKey, volumePropertyValue);
        }

        Map<String, Object> volumeMap = ((ComplexPropertyValue) volumePropertyValue).getValue();
        volumeMap.put(AZ_KEY, availabilityZone);

        PaaSInstancePersistentResourceMonitorEvent event = new PaaSInstancePersistentResourceMonitorEvent(paaSNodeTemplate.getId(), null, handlerPropertyKey,
                volumeMap);
        event.setDate(new Date().getTime());
        event.setDeploymentId(deploymentId);
        eventService.registerEvent(event);
    }

    // Sort the members by volume with AZ to re-use this AZ in priority
    private LinkedHashSet<String> sortMembersByVolumeAndAZ(Map<String, PaaSNodeTemplate> allNodes, Set<String> members) {
        LinkedHashSet<String> membersWithVolumesAndAZ = new LinkedHashSet<>();
        LinkedHashSet<String> membersWithoutVolumes = new LinkedHashSet<>();
        for (String member : members) {
            boolean hasAZ = false;
            PaaSNodeTemplate target = allNodes.get(member);
            if (target.getStorageNodes() != null) {
                for (PaaSNodeTemplate volumeNode : target.getStorageNodes()) {
                    if (getAZ(volumeNode, VOLUME_PROPERTY) != null) {
                        hasAZ = true;
                        break;
                    }
                }
            }
            if (hasAZ) {
                membersWithVolumesAndAZ.add(member);
            } else {
                membersWithoutVolumes.add(member);
            }
        }
        membersWithVolumesAndAZ.addAll(membersWithoutVolumes);
        return membersWithVolumesAndAZ;
    }

    private Map<String, Integer> initMapOfUsedAZ(List<LocationResourceTemplate> availabilityZones) {
        Map<String, Integer> countUsageOfAZ = new HashMap<>();
        for (LocationResourceTemplate availabilityZone : availabilityZones ) {
            countUsageOfAZ.put(((ScalarPropertyValue) availabilityZone.getTemplate().getProperties().get("id")).getValue(), 0);
        }
        return countUsageOfAZ;
    }

    private String getLessUsedAZ(Map<String, Integer> countUsageOfAZ) {
        String lessUsedAZ = countUsageOfAZ.keySet().iterator().next();
        for (Map.Entry<String, Integer> entry : countUsageOfAZ.entrySet()) {
            if (entry.getValue() < countUsageOfAZ.get(lessUsedAZ)) {
                lessUsedAZ = entry.getKey();
            }
        }
        return lessUsedAZ;
    }
}