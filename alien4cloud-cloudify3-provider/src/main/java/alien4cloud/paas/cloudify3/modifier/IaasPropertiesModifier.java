package alien4cloud.paas.cloudify3.modifier;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import alien4cloud.model.orchestrators.OrchestratorConfiguration;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.services.OrchestratorConfigurationService;
import alien4cloud.paas.cloudify3.configuration.CloudConfiguration;
import alien4cloud.paas.cloudify3.configuration.LocationConfiguration;
import alien4cloud.paas.cloudify3.configuration.OpenstackLocationConfiguration;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.tosca.context.ToscaContextual;
import com.google.common.collect.Maps;
import lombok.extern.java.Log;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.TopologyModifierSupport;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.normative.constants.NormativeComputeConstants;
import org.alien4cloud.tosca.utils.TopologyNavigationUtil;

/**
 * TODO: add logs using FlowExecutionContext
 */
@Log
public class IaasPropertiesModifier extends TopologyModifierSupport {

    @Inject
    private OrchestratorConfigurationService orchestratorConfigurationService;

    @Override
    @ToscaContextual
    public void process(Topology topology, FlowExecutionContext context) {
        log.info("Processing topology " + topology.getId());
        try {
            Map<String, Location> locations = (Map<String, Location>) context.getExecutionCache().get(FlowExecutionContext.DEPLOYMENT_LOCATIONS_MAP_CACHE_KEY);
            for (Location location : locations.values()) {
                OrchestratorConfiguration orchestrator = orchestratorConfigurationService.getConfigurationOrFail(location.getOrchestratorId());
                CloudConfiguration config = JsonUtil.readObject(JsonUtil.toString(orchestrator.getConfiguration()), CloudConfiguration.class);
                switch (location.getInfrastructureType()) {
                case "openstack":
                    log.info("Processing with openstack location");
                    processOpenstackLocation(topology, config.getLocations().getOpenstack(), context);
                    break;
                case "amazon":
                    log.info("Processing with amazon location");
                    break;
                default:
                    log.warning("Unknown location: " + location.getInfrastructureType());
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processOpenstackLocation(Topology topology, OpenstackLocationConfiguration openstack, FlowExecutionContext context) {
        try {
            Set<NodeTemplate> computeTemplates = TopologyNavigationUtil.getNodesOfType(topology, NormativeComputeConstants.COMPUTE_TYPE, true);
            for (NodeTemplate node : computeTemplates) {
                this.mergeAgentConfig(node, openstack, context);
                this.mergeOpenstackManagerResources(node, openstack, context);
            }
        } catch (Exception e) {
            context.getLog().error("Couldn't process iaas-property-modifier");
            log.log(Level.WARNING, "Couldn't process iaas-property-modifier", e);
        }
    }

    private void mergeOpenstackManagerResources(NodeTemplate node, OpenstackLocationConfiguration locationConfig, FlowExecutionContext context) {
        Map<String, AbstractPropertyValue> properties = node.getProperties();
        // Security groups
        if (this.keyDoNotExistOrIsNull(properties, "server")) {
            ComplexPropertyValue cpv = new ComplexPropertyValue();
            cpv.setValue(Maps.newHashMap());
            properties.put("server", cpv);
        }
        if (this.keyDoNotExistOrIsNull(((ComplexPropertyValue) properties.get("server")).getValue(), "security_groups")) {
            ((ComplexPropertyValue) properties.get("server")).getValue().put("security_groups", new ArrayList<AbstractPropertyValue>());
        }
        List<String> secgroups = (List<String>) ((ComplexPropertyValue) properties.get("server")).getValue().get("security_groups");
        if (!secgroups.contains(locationConfig.getSecurityGroupName())) {
            secgroups.add(locationConfig.getSecurityGroupName());
            context.getLog().info(
                    String.format("Added security group %s to the <%s> node with cloud configuration value", locationConfig.getSecurityGroupName(),
                            node.getName()));
        }

        // Managerment network name
        if (this.keyDoNotExistOrIsNull(properties, "management_network_name")) {
            properties.put("management_network_name", new ScalarPropertyValue(locationConfig.getManagerNetworkName()));
            context.getLog().info(String.format("Overrided management_network_name for the <%s> node with cloud configuration value", node.getName()));
        }
    }

    private void mergeAgentConfig(NodeTemplate node, LocationConfiguration locationConfig, FlowExecutionContext context) {
        Map<String, AbstractPropertyValue> properties = node.getProperties();
        if (locationConfig.getAgentConfig() != null) {
            if (this.keyDoNotExistOrIsNull(properties, "agent_config")) {
                ComplexPropertyValue accp = new ComplexPropertyValue();
                accp.setValue(Maps.newHashMap());
                properties.put("agent_config", accp);
            }
            ComplexPropertyValue ac = ((ComplexPropertyValue) properties.get("agent_config"));
            if (this.keyDoNotExistOrIsNull(properties, "user")) {
                properties.put("user", new ScalarPropertyValue(locationConfig.getAgentConfig().getUser()));
                context.getLog().info(String.format("Overrided agent_config.user for the <%s> node with cloud configuration value", node.getName()));
            }
            if (this.keyDoNotExistOrIsNull(ac.getValue(), "key")) {
                ac.getValue().put("key", new ScalarPropertyValue(locationConfig.getAgentConfig().getKey()));
                context.getLog().info(String.format("Overrided agent_config.key for the <%s> node with cloud configuration value", node.getName()));
            }
        }
    }

    private boolean keyDoNotExistOrIsNull(Map map, String key) {
        return !map.containsKey(key) || map.get(key) == null;
    }
}
