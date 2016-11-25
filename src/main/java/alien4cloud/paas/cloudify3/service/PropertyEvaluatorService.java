package alien4cloud.paas.cloudify3.service;

import java.util.List;
import java.util.Map;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.ConcatPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.IValue;
import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.definitions.Operation;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.templates.Capability;
import org.springframework.stereotype.Component;

import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.paas.IPaaSTemplate;
import alien4cloud.paas.function.FunctionEvaluator;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;
import alien4cloud.tosca.ToscaUtils;
import alien4cloud.tosca.normative.ToscaFunctionConstants;

@Component("property-evaluator-service")
public class PropertyEvaluatorService {

    private void processAttributes(Map<String, IValue> attributes, IPaaSTemplate node, Map<String, PaaSNodeTemplate> allNodes) {
        if (attributes != null) {
            for (Map.Entry<String, IValue> attributeEntry : attributes.entrySet()) {
                attributeEntry.setValue(process(attributeEntry.getValue(), node, allNodes));
            }
        }
    }

    private void processInterfaces(Map<String, Interface> interfaces, IPaaSTemplate node, Map<String, PaaSNodeTemplate> allNodes) {
        if (interfaces != null) {
            for (Interface interfazz : interfaces.values()) {
                Map<String, Operation> operations = interfazz.getOperations();
                if (operations != null) {
                    for (Operation operation : operations.values()) {
                        Map<String, IValue> inputs = operation.getInputParameters();
                        if (inputs != null) {
                            for (Map.Entry<String, IValue> inputEntry : inputs.entrySet()) {
                                inputEntry.setValue(process(inputEntry.getValue(), node, allNodes));
                            }
                        }
                    }
                }
            }
        }
    }

    private void processCapability(Capability capability, IPaaSTemplate node, Map<String, PaaSNodeTemplate> allNodes) {
        if (capability != null && capability.getProperties() != null) {
            for (Map.Entry<String, AbstractPropertyValue> attributeEntry : capability.getProperties().entrySet()) {
                attributeEntry.setValue((AbstractPropertyValue) process(attributeEntry.getValue(), node, allNodes));
            }
        }
    }

    /**
     * Process the deployment topology, every get_property will be replaced by its own static value retrieved from the topology
     *
     * @param deploymentContext the deployment context
     */
    public void processGetPropertyFunction(PaaSTopologyDeploymentContext deploymentContext) {
        Map<String, PaaSNodeTemplate> allNodes = deploymentContext.getPaaSTopology().getAllNodes();
        for (PaaSNodeTemplate node : allNodes.values()) {
            processAttributes(node.getTemplate().getAttributes(), node, allNodes);
            processInterfaces(node.getInterfaces(), node, allNodes);
            List<PaaSRelationshipTemplate> relationships = node.getRelationshipTemplates();
            if (relationships != null) {
                for (PaaSRelationshipTemplate relationship : relationships) {
                    processAttributes(relationship.getTemplate().getAttributes(), relationship, allNodes);
                    processInterfaces(relationship.getInterfaces(), relationship, allNodes);
                }
            }
            if (node.getTemplate().getCapabilities() != null) {
                for (Map.Entry<String, Capability> capabilityEntry : node.getTemplate().getCapabilities().entrySet()) {
                    processCapability(capabilityEntry.getValue(), node, allNodes);
                }
            }
        }
    }

    /**
     * Process an IValue (it can be a scalar value, a get_property, get_attribute, get_operation_output or a concat) and replace all get_property occurrence
     * with its value and return the new IValue with get_property replaced by its value
     *
     * @param value the value to be processed
     * @return the new value with get_property processed
     */
    private IValue process(IValue value, IPaaSTemplate node, Map<String, PaaSNodeTemplate> allNodes) {
        if (value instanceof FunctionPropertyValue) {
            return processSimpleFunction((FunctionPropertyValue) value, node, allNodes);
        } else if (value instanceof ConcatPropertyValue) {
            return processConcatFunction((ConcatPropertyValue) value, node, allNodes);
        } else {
            return value;
        }
    }

    private IValue processSimpleFunction(FunctionPropertyValue value, IPaaSTemplate node, Map<String, PaaSNodeTemplate> allNodes) {
        if (ToscaFunctionConstants.GET_PROPERTY.equals(value.getFunction())) {
            String evaluatedValue = evaluateGetPropertyFunction(value, node, allNodes);
            return new ScalarPropertyValue(evaluatedValue);
        } else {
            return value;
        }
    }

    /**
     * <p>
     * !!! Copied and adapted from marathon plugin !!!
     * Search for a property of a capability being required as a target of a relationship.
     * <p/>
     * 
     * @param value the function parameters, e.g. the requirement name & property name to lookup.
     * @param node The source node of the relationships, which defines the requirement.
     * @param allNodes all the nodes of the topology.
     * @return a String representing the property value.
     */
    private String evaluateGetPropertyFunction(FunctionPropertyValue value, IPaaSTemplate node, Map<String, PaaSNodeTemplate> allNodes) {

        if(value.getParameters().contains("REQ_TARGET")) {
            // Search for the requirement's target by filter the relationships' templates of this node.
            // If a target is found, then lookup for the given property name in its capabilities.
            // The orchestrator replaces the PORT and IP_ADDRESS by the target's service port and the load balancer hostname respectively.
            String requirementName = value.getCapabilityOrRequirementName();
            String propertyName = value.getElementNameToFetch();
            if (node instanceof PaaSNodeTemplate && requirementName != null) {
                for (PaaSRelationshipTemplate relationshipTemplate : ((PaaSNodeTemplate) node).getRelationshipTemplates()) {
                    if (node.getId().equals(relationshipTemplate.getSource()) && requirementName.equals(relationshipTemplate.getTemplate().getRequirementName())) {
                        if (relationshipTemplate.instanceOf("tosca.relationships.ConnectsTo")) {
                            if ("port".equalsIgnoreCase(propertyName) || "ip_address".equalsIgnoreCase(propertyName)) {
                                // FIXME ugly workaround. If the property is 'ip_address', change it to 'ip' (cfy3)
                                if("ip_address".equalsIgnoreCase(propertyName)) {
                                    PaaSNodeTemplate target = allNodes.get(relationshipTemplate.getTemplate().getTarget());
                                    if(ToscaUtils.isFromType(BlueprintService.TOSCA_NODES_CONTAINER_APPLICATION_DOCKER_CONTAINER, target.getIndexedToscaElement())){
                                        propertyName = "clusterIP";
                                    } else {
                                        propertyName = "ip";
                                    }
                                }
                                // Return a string as "function_name, node_id, propertiy_name" (c.f. kubernetes.yaml.vm)
                                return String.format("%s,%s,%s", value.getFunction(), relationshipTemplate.getTemplate().getTarget(), propertyName);
                            }
                        }
                    }
                }
            }
        }
        // Nominal case : get the requirement's targeted capability property.
        // TODO: Add the REQ_TARGET keyword in the evaluateGetProperty function soo this is evaluated at parsing
        return FunctionEvaluator.evaluateGetPropertyFunction(value, node, allNodes);
    }

    private IValue processConcatFunction(ConcatPropertyValue concatPropertyValue, IPaaSTemplate node, Map<String, PaaSNodeTemplate> allNodes) {
        if (concatPropertyValue.getParameters() == null || concatPropertyValue.getParameters().isEmpty()) {
            throw new InvalidArgumentException("Parameter list for concat function is empty");
        }
        for (int i = 0; i < concatPropertyValue.getParameters().size(); i++) {
            IValue concatParam = concatPropertyValue.getParameters().get(i);
            if (concatParam instanceof FunctionPropertyValue) {
                concatPropertyValue.getParameters().set(i, processSimpleFunction((FunctionPropertyValue) concatParam, node, allNodes));
            }
        }
        return concatPropertyValue;
    }
}
