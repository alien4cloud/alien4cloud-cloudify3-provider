package alien4cloud.paas.cloudify3.blueprint;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import alien4cloud.model.common.Tag;
import alien4cloud.paas.cloudify3.configuration.MappingConfiguration;
import alien4cloud.paas.cloudify3.error.BadConfigurationException;
import alien4cloud.paas.cloudify3.service.PropertyEvaluatorService;
import alien4cloud.paas.cloudify3.service.model.CloudifyDeployment;
import alien4cloud.paas.cloudify3.util.mapping.IPropertyMapping;
import alien4cloud.paas.cloudify3.util.mapping.PropertiesMappingUtil;
import alien4cloud.paas.cloudify3.util.mapping.PropertyValueUtil;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.tosca.serializer.ToscaPropertySerializerUtils;
import alien4cloud.utils.TagUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.IValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.normative.constants.NormativeCapabilityTypes;
import org.alien4cloud.tosca.normative.constants.NormativeComputeConstants;
import org.alien4cloud.tosca.utils.ToscaTypeUtils;

import static alien4cloud.utils.AlienUtils.safe;

public class NativeTypeGenerationUtil extends AbstractGenerationUtil {

    public static final String MAPPED_TO_KEY = "_a4c_c3_derived_from";

    public NativeTypeGenerationUtil(MappingConfiguration mappingConfiguration, CloudifyDeployment alienDeployment, Path recipePath,
            PropertyEvaluatorService propertyEvaluatorService) {
        super(mappingConfiguration, alienDeployment, recipePath, propertyEvaluatorService);
    }

    public static Map<String, AbstractPropertyValue> addPropertyValueIfMissing(Map<String, AbstractPropertyValue> properties, String key, String value) {
        Map<String, AbstractPropertyValue> copy = new HashMap<>(properties);
        if (!copy.containsKey(key) || copy.get(key) == null) {
            copy.put(key, new ScalarPropertyValue(value));
        }
        return copy;
    }

    /**
     * Utility method used by velocity generator in order to find the cloudify type from a cloudify tosca type.
     *
     * @param toscaNodeType The tosca node type.
     * @return The matching cloudify's type.
     */
    public String mapToCloudifyType(NodeType toscaNodeType) {
        String cloudifyType = TagUtil.getTagValue(toscaNodeType.getTags(), MAPPED_TO_KEY);
        if (cloudifyType == null) {
            throw new BadConfigurationException("In the type " + toscaNodeType.getElementId() + " the tag " + MAPPED_TO_KEY
                    + " is mandatory in order to know which cloudify native type to map to");
        }
        return cloudifyType;
    }

    /**
     * Apply properties mapping and then format properties for cloudify blueprint.
     *
     * @param indentLevel  The indentation level for the properties.
     * @param properties   The properties values map.
     * @param propMappings The mapping configuration to map values.
     * @return The formatted properties string to insert in the blueprint.
     */
    public String formatProperties(int indentLevel, Map<String, AbstractPropertyValue> properties, Map<String, List<IPropertyMapping>> propMappings) {
        Map<String, AbstractPropertyValue> mappedProperties = PropertyValueUtil.mapProperties(propMappings, properties);
        return ToscaPropertySerializerUtils.formatProperties(indentLevel, mappedProperties);
    }

    public String formatProperties(int indentLevel, Map<String, AbstractPropertyValue> properties,
            Map<String, Map<String, List<IPropertyMapping>>> propertyMappings, String nodeType) {
        Map<String, AbstractPropertyValue> mappedProperties = PropertyValueUtil.mapProperties(propertyMappings, nodeType, properties);
        return ToscaPropertySerializerUtils.formatProperties(indentLevel, mappedProperties);
    }

    public Map<String, List<IPropertyMapping>> loadPropertyMapping(NodeType type, String tagName) {
        return PropertiesMappingUtil.loadPropertyMapping(tagName, type);
    }

    public Map<String, FunctionPropertyValue> getAttributesMapping(Map<String, IValue> attributes) {
        Map<String, FunctionPropertyValue> functions = Maps.newLinkedHashMap();
        for (Map.Entry<String, IValue> attributeEntry : attributes.entrySet()) {
            if (attributeEntry.getValue() instanceof FunctionPropertyValue) {
                functions.put(attributeEntry.getKey(), (FunctionPropertyValue) attributeEntry.getValue());
            }
        }
        return functions;
    }

    /**
     * Get the value of the _a4c_persistent_resources tag.
     *
     * @param tags The list of tags in which to search.
     * @return The value of the _a4c_persistent_resources tag or null if the tag is not present in the list.
     */
    public String getPersistentResourceId(List<Tag> tags) {
        return TagUtil.getTagValue(tags, CustomTags.PERSISTENT_RESOURCE_TAG);
    }

    public String getResourceIdKey(List<Tag> tags) {
        return TagUtil.getTagValue(tags, CustomTags.RESOURCE_ID_KEY_TAG);
    }

    /**
     * @return all computes, natives (managed by cfy) and custom.
     */
    public List<PaaSNodeTemplate> getAllComputes() {
        List<PaaSNodeTemplate> result = Lists.newArrayList(this.alienDeployment.getComputes());
        for (PaaSNodeTemplate node : this.alienDeployment.getCustomResources().values()) {
            boolean isCompute = ToscaTypeUtils.isOfType(node.getIndexedToscaElement(), NormativeComputeConstants.COMPUTE_TYPE);
            if (isCompute) {
                result.add(node);
            }
        }
        return result;
    }

    /**
     * Alien4cloud currently consider as scalable every node that does not have a host and that has the scalable capability.
     *
     * @return Get all nodes that does not .
     */
    public List<PaaSNodeTemplate> getAllScalableNodes() {
        List<PaaSNodeTemplate> result = Lists.newArrayList(this.alienDeployment.getComputes());
        for (PaaSNodeTemplate node : this.alienDeployment.getCustomResources().values()) {
            if (isScalableNode(node.getIndexedToscaElement())) {
                result.add(node);
            }
        }
        return result;
    }

    private boolean isScalableNode(NodeType nodeType) {
        for (CapabilityDefinition capabilityDefinition : safe(nodeType.getCapabilities())) {
            CapabilityType capabilityType = this.alienDeployment.getCapabilityTypes().get(capabilityDefinition.getType());
            if (ToscaTypeUtils.isOfType(capabilityType, NormativeCapabilityTypes.SCALABLE)) {
                return true;
            }
        }
        return false;
    }

    public String formatObjectProperties(int indentLevel, Object object, List<String> ignoredFields) {
        StringBuilder buffer = new StringBuilder();
        try {
            Field[] fields = Class.forName(object.getClass().getName()).getDeclaredFields();
            for (Field f : fields) {
                if (ignoredFields == null || !ignoredFields.contains(f.getName())) {
                    f.setAccessible(true);
                    Object value = f.get(object);
                    if (value != null) {
                        buffer.append(ToscaPropertySerializerUtils.indent(indentLevel)).append(f.getName()).append(": ").append(f.get(object)).append("\n");
                    }
                }
            }
        } catch (Exception e) {
            throw new BadConfigurationException("Couldn't serialize iaas configuration");
        }
        return buffer.toString();
    }

}
