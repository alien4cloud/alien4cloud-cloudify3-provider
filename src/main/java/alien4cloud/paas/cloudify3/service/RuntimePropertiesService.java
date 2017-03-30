package alien4cloud.paas.cloudify3.service;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import alien4cloud.paas.cloudify3.configuration.MappingConfigurationHolder;
import alien4cloud.paas.cloudify3.model.AbstractCloudifyModel;
import alien4cloud.paas.cloudify3.model.Node;
import alien4cloud.paas.cloudify3.model.NodeInstance;
import alien4cloud.paas.cloudify3.model.Relationship;
import alien4cloud.paas.cloudify3.model.RelationshipInstance;
import alien4cloud.paas.cloudify3.restclient.NodeClient;
import alien4cloud.paas.cloudify3.restclient.NodeInstanceClient;
import alien4cloud.paas.exception.NotSupportedException;
import org.alien4cloud.tosca.normative.constants.NormativeRelationshipConstants;
import org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants;
import alien4cloud.utils.MapUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * This service can be used to retrieve runtime properties of running instances from a deployment
 */
@Component
@Slf4j
public class RuntimePropertiesService {

    @Resource
    private NodeInstanceClient nodeInstanceClient;

    @Resource
    private NodeClient nodeClient;

    @Resource
    private MappingConfigurationHolder mappingConfigurationHolder;

    public ListenableFuture<Map<String, Object>> evaluate(String deploymentId, final String nodeName, final String attributeName) {
        ListenableFuture<NodeInstance[]> futureNodeInstances = nodeInstanceClient.asyncList(deploymentId);
        ListenableFuture<Node[]> futureNodes = nodeClient.asyncList(deploymentId, nodeName);
        ListenableFuture<List<AbstractCloudifyModel[]>> nodeAndNodeInstancesFutures = Futures.allAsList(futureNodeInstances, futureNodes);

        Function<List<AbstractCloudifyModel[]>, Map<String, Object>> adapter = new Function<List<AbstractCloudifyModel[]>, Map<String, Object>>() {

            @Override
            public Map<String, Object> apply(List<AbstractCloudifyModel[]> nodeAndNodeInstances) {
                NodeInstance[] nodeInstances = (NodeInstance[]) nodeAndNodeInstances.get(0);
                Node[] nodes = (Node[]) nodeAndNodeInstances.get(1);
                Map<String, Node> nodeMap = Maps.newHashMap();
                for (Node node : nodes) {
                    nodeMap.put(node.getId(), node);
                }

                Map<String, NodeInstance> nodeInstanceMap = Maps.newHashMap();
                for (NodeInstance instance : nodeInstances) {
                    nodeInstanceMap.put(instance.getId(), instance);
                }

                Map<String, Object> evalResult = Maps.newHashMap();
                for (NodeInstance nodeInstance : nodeInstances) {
                    if (nodeInstance.getNodeId().equals(nodeName)) {
                        Node node = nodeMap.get(nodeInstance.getNodeId());
                        Map<String, Map<String, Object>> mappingConfigurations = getAttributesMappingConfiguration(node.getProperties());
                        evalResult.put(nodeInstance.getId(),
                                getAttributeValue(attributeName, mappingConfigurations, node, nodeInstance, nodeMap, nodeInstanceMap));
                    }
                }
                return evalResult;
            }
        };
        return Futures.transform(nodeAndNodeInstancesFutures, adapter);
    }

    private Map<String, Map<String, Object>> getAttributesMappingConfiguration(Map<String, Object> properties) {
        Map<String, Map<String, Object>> mappingConfigurations = Maps.newHashMap();
        for (Map.Entry<String, Object> propertyEntry : properties.entrySet()) {
            if (propertyEntry.getKey().startsWith("_a4c_att_")) {
                mappingConfigurations.put(propertyEntry.getKey().substring("_a4c_att_".length()), (Map<String, Object>) propertyEntry.getValue());
            }
        }
        return mappingConfigurations;
    }

    private RelationshipInstance getRelationshipInstance(NodeInstance instance, Relationship relationship) {
        for (RelationshipInstance relationshipInstance : instance.getRelationships()) {
            if (relationshipInstance.getTargetName().equals(relationship.getTargetId())) {
                return relationshipInstance;
            }
        }
        return null;
    }

    public Map<String, String> getAttributes(Node node, NodeInstance instance, Map<String, Node> nodeMap, Map<String, NodeInstance> nodeInstanceMap) {
        Map<String, Object> properties = node.getProperties();
        Map<String, Object> attributes = Maps.newHashMap();
        Map<String, Map<String, Object>> mappingConfigurations = getAttributesMappingConfiguration(properties);
        if (MapUtils.isNotEmpty(mappingConfigurations)) {
            // If mapping configurations found, only take those configured
            // It's native types component
            for (Map.Entry<String, Map<String, Object>> mappingConfiguration : mappingConfigurations.entrySet()) {
                attributes.put(mappingConfiguration.getKey(),
                        getMappedAttributeValue(mappingConfiguration.getKey(), mappingConfigurations, node, instance, nodeMap, nodeInstanceMap));
            }
        } else {
            // If no mapping found --> take everything as if
            attributes = instance.getRuntimeProperties();
        }
        try {
            return MapUtil.toString(attributes);
        } catch (JsonProcessingException e) {
            log.error("Unable to stringify attributes", e);
            return null;
        }
    }

    private Relationship getRelationshipOfType(Node node, String type) {
        for (Relationship relationship : node.getRelationships()) {
            if (relationship.getTypeHierarchy().contains(type)) {
                return relationship;
            }
        }
        return null;
    }

    private Object doGetAttributeValue(String attributeName, Map<String, Map<String, Object>> mappingConfigurations, Node node, NodeInstance instance,
            Map<String, Node> nodeMap, Map<String, NodeInstance> nodeInstanceMap) {
        Object attributeValue = instance.getRuntimeProperties().get(attributeName);
        if (attributeValue == null) {
            Relationship relationship = getRelationshipOfType(node,
                    mappingConfigurationHolder.getMappingConfiguration().getNormativeTypes().get(NormativeRelationshipConstants.HOSTED_ON));
            // Redirect to the parent
            if (relationship != null) {
                Node targetNode = nodeMap.get(relationship.getTargetId());
                RelationshipInstance relationshipInstance = getRelationshipInstance(instance, relationship);
                NodeInstance targetInstance = nodeInstanceMap.get(relationshipInstance.getTargetId());
                return getAttributeValue(attributeName, mappingConfigurations, targetNode, targetInstance, nodeMap, nodeInstanceMap);
            }
        } else {
            return attributeValue;
        }
        return null;
    }

    private Object getAttributeValue(String attributeName, Map<String, Map<String, Object>> mappingConfigurations, Node node, NodeInstance instance,
            Map<String, Node> nodeMap, Map<String, NodeInstance> nodeInstanceMap) {
        if (mappingConfigurations.containsKey(attributeName)) {
            return getMappedAttributeValue(attributeName, mappingConfigurations, node, instance, nodeMap, nodeInstanceMap);
        } else {
            return doGetAttributeValue(attributeName, mappingConfigurations, node, instance, nodeMap, nodeInstanceMap);
        }
    }

    private Object getMappedAttributeValue(String attributeName, Map<String, Map<String, Object>> mappingConfigurations, Node node, NodeInstance instance,
            Map<String, Node> nodeMap, Map<String, NodeInstance> nodeInstanceMap) {
        Map<String, Object> mappingConfiguration = mappingConfigurations.get(attributeName);
        if (mappingConfiguration == null) {
            throw new NotSupportedException(attributeName + " is not a mapped attribute");
        }
        Object parametersObject = mappingConfiguration.get("parameters");
        if (!(parametersObject instanceof List)) {
            throw new NotSupportedException("Mapping configuration invalid " + mappingConfiguration);
        }
        List<String> parameters = (List<String>) mappingConfiguration.get("parameters");
        if (parameters.isEmpty()) {
            throw new NotSupportedException("Mapping configuration invalid " + mappingConfiguration + ", parameters are empty");
        }
        String function = (String) mappingConfiguration.get("function");
        if (ToscaFunctionConstants.GET_ATTRIBUTE.equals(function)) {
            String entity = parameters.get(0);
            if (ToscaFunctionConstants.SELF.equals(entity)) {
                if (parameters.size() != 2) {
                    throw new NotSupportedException(
                            "Mapping configuration invalid " + mappingConfiguration + ", parameters must be SELF + name of the attribute");
                }
                String fromAttribute = parameters.get(1);
                if (!fromAttribute.equals(attributeName)) {
                    // This way it will not loop infinitely
                    return getAttributeValue(fromAttribute, mappingConfigurations, node, instance, nodeMap, nodeInstanceMap);
                } else {
                    return doGetAttributeValue(fromAttribute, mappingConfigurations, node, instance, nodeMap, nodeInstanceMap);
                }
            } else if (ToscaFunctionConstants.TARGET.equals(entity)) {
                if (parameters.size() != 3) {
                    throw new NotSupportedException("Mapping configuration invalid " + mappingConfiguration
                            + ", parameters must be TARGET + relationship type + name of the attribute");
                }
                String relationshipType = parameters.get(1);
                Relationship relationship = getRelationshipOfType(node, relationshipType);
                if (relationship != null) {
                    Node targetNode = nodeMap.get(relationship.getTargetId());
                    RelationshipInstance relationshipInstance = getRelationshipInstance(instance, relationship);
                    NodeInstance targetInstance = nodeInstanceMap.get(relationshipInstance.getTargetId());
                    return getAttributeValue(parameters.get(2), mappingConfigurations, targetNode, targetInstance, nodeMap, nodeInstanceMap);
                }
            } else {
                throw new NotSupportedException("TARGET or SELF are the only entities supported for the moment for attribute mapping");
            }
        } else {
            throw new NotSupportedException("get_attribute is the only one supported for the moment for attribute mapping");
        }
        return null;
    }
}
