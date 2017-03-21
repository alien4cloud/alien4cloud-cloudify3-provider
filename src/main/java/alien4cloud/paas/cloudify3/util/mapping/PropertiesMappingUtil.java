package alien4cloud.paas.cloudify3.util.mapping;

import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import alien4cloud.paas.wf.WorkflowsBuilderService.TopologyContext;
import org.alien4cloud.tosca.normative.types.ToscaTypes;
import alien4cloud.utils.TagUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

/**
 * Perform mapping of properties
 */
@Slf4j
@Component
public class PropertiesMappingUtil {
    public static final String PROP_MAPPING_TAG_KEY = "_a4c_c3_prop_map";
    private static ObjectMapper mapper = new ObjectMapper();

    /**
     * Load the property mappings defined in tags
     *
     * @param nodeTypes
     *            The list of node types for which to extract property mappings.
     * @param topologyContext
     * @return A map <nodeType, <toscaPath, cloudifyPath>>>
     */
    public static Map<String, Map<String, List<IPropertyMapping>>> loadPropertyMappings(List<NodeType> nodeTypes, TopologyContext topologyContext) {
        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
        };

        // <nodeType, <toscaPath, cloudifyPath>>>
        Map<String, Map<String, List<IPropertyMapping>>> propertyMappingsByTypes = Maps.newLinkedHashMap();
        for (NodeType nodeType : nodeTypes) {
            deeplyLoadPropertyMapping(PROP_MAPPING_TAG_KEY, propertyMappingsByTypes, nodeType, topologyContext);
        }
        return propertyMappingsByTypes;
    }

    /**
     * deeply load properties mappings for a type. This includes the mappings for the types, and eventually his properties dataTypes
     *
     * @param propertyMappingsByTypes
     * @param inheritableToscaElement
     * @param topologyContext
     */
    private static void deeplyLoadPropertyMapping(String fromTag, Map<String, Map<String, List<IPropertyMapping>>> propertyMappingsByTypes,
                                                  AbstractInheritableToscaType inheritableToscaElement, TopologyContext topologyContext) {
        // do not proceed if mapping already exists
        if (inheritableToscaElement == null || propertyMappingsByTypes.containsKey(inheritableToscaElement.getElementId())) {
            return;
        }

        Map<String, List<IPropertyMapping>> mappings = loadPropertyMapping(fromTag, inheritableToscaElement);
        if (MapUtils.isNotEmpty(mappings)) {
            propertyMappingsByTypes.put(inheritableToscaElement.getElementId(), mappings);
        }

        loadPropertiesDataTypesMapping(fromTag, propertyMappingsByTypes, inheritableToscaElement, topologyContext);
    }

    /**
     * A property Data type can also have properties mapping definition. Load it, and add a marker on the related property
     *
     * @param fromTag
     *
     * @param propertyMappingsByTypes
     * @param inheritableToscaElement
     * @param topologyContext
     */
    private static void loadPropertiesDataTypesMapping(String fromTag, Map<String, Map<String, List<IPropertyMapping>>> propertyMappingsByTypes,
                                                       AbstractInheritableToscaType inheritableToscaElement, TopologyContext topologyContext) {
        if (MapUtils.isEmpty(inheritableToscaElement.getProperties())) {
            return;
        }
        for (Entry<String, PropertyDefinition> definitionEntry : inheritableToscaElement.getProperties().entrySet()) {
            // build the marker for the property
            ComplexPropertyMapping mapping = buildPropertyMapping(definitionEntry.getValue());

            if (mapping != null) {
                AbstractInheritableToscaType dataType = (AbstractInheritableToscaType) topologyContext.findElement(AbstractToscaType.class,
                        mapping.getType());
                // if dataType found in repository, then try to load its mapping
                if (dataType != null) {
                    deeplyLoadPropertyMapping(fromTag, propertyMappingsByTypes, dataType, topologyContext);
                    // only register the marker if there was mapping added for the data type
                    if (propertyMappingsByTypes.containsKey(dataType.getElementId())) {
                        Map<String, List<IPropertyMapping>> typeMappings = propertyMappingsByTypes.get(inheritableToscaElement.getElementId());
                        if (typeMappings == null) {
                            typeMappings = Maps.newLinkedHashMap();
                            propertyMappingsByTypes.put(inheritableToscaElement.getElementId(), typeMappings);
                        }
                        // Do not override the existing mapping, copy it in the complex mapping instead
                        completeMappingMarker(mapping, definitionEntry.getKey(), typeMappings);
                        ArrayList<IPropertyMapping> mappingList = new ArrayList<IPropertyMapping>();
                        mappingList.add(mapping);

                        typeMappings.put(definitionEntry.getKey(), mappingList);
                    }
                }
            }
        }
    }

    private static void completeMappingMarker(ComplexPropertyMapping mapping, String key, Map<String, List<IPropertyMapping>> typeMappings) {
        if (typeMappings.get(key) != null) {
            mapping.setRelatedSimplePropertiesMapping(typeMappings.get(key));
        }
    }

    private static ComplexPropertyMapping buildPropertyMapping(PropertyDefinition definition) {

        if (ToscaTypes.isSimple(definition.getType())) {
            return null;
        }

        switch (definition.getType()) {
        case ToscaTypes.LIST:
        case ToscaTypes.MAP:
            return new ComplexPropertyMapping(definition.getEntrySchema().getType(), ToscaTypes.LIST.equalsIgnoreCase(definition.getType()));
        default:
            return new ComplexPropertyMapping(definition.getType(), false);
        }
    }

    public static Map<String, List<IPropertyMapping>> loadPropertyMapping(String fromTagName, AbstractInheritableToscaType toscaElement) {
        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
        };
        String mappingStr = TagUtil.getTagValue(toscaElement.getTags(), fromTagName);
        if (mappingStr == null) {
            return Maps.newLinkedHashMap();
        }
        try {
            Map<String, Object> mappingsDef = mapper.readValue(mappingStr, typeRef);
            return fromFullPathMap(mappingsDef, toscaElement);
        } catch (IOException e) {
            log.error("Failed to load property mapping for tosca element " + toscaElement.getElementId() + ", will be ignored", e);
            return Maps.newLinkedHashMap();
        }
    }

    private static Map<String, List<IPropertyMapping>> fromFullPathMap(Map<String, Object> parsedMappings, AbstractInheritableToscaType toscaElement) {
        Map<String, List<IPropertyMapping>> propertyMappings = Maps.newLinkedHashMap();

        for (Map.Entry<String, Object> parsedMapping : parsedMappings.entrySet()) {
            String[] key = asPropAndSubPath(parsedMapping.getKey());

            ArrayList<IPropertyMapping> propertyMappingsL = new ArrayList<IPropertyMapping>();
            if (parsedMapping.getValue() != null) {
                List<Object> mappingList;
                if (parsedMapping.getValue() instanceof List) {
                    mappingList = (List<Object>) parsedMapping.getValue();
                } else { // a "single mapping" is a "list of one mapping"
                    mappingList = new ArrayList<Object>(Arrays.asList(parsedMapping.getValue()));
                }

                for (Object mappingItem : mappingList) {
                    PropertyMapping propertyMapping = (PropertyMapping) propertyMappings.get(key[0]);
                    if (propertyMapping == null) {
                        propertyMapping = new PropertyMapping();
                    }
                    SourceMapping sourceMapping = new SourceMapping(key[1], toscaElement.getProperties().get(key[0]));
                    TargetMapping targetMapping = new TargetMapping();
                    String mappingString;
                    if (mappingItem instanceof String) {
                        mappingString = (String) mappingItem;
                    } else {

                        Map<String, String> complexMapping = (Map<String, String>) mappingItem;
                        mappingString = complexMapping.get("path");
                        targetMapping.setUnit(complexMapping.get("unit"));

                        if (complexMapping.containsKey("ceil")) {
                            targetMapping.setCeil(true);
                        }
                    }
                    String[] splitMappingString = asPropAndSubPath(mappingString);
                    targetMapping.setProperty(splitMappingString[0]);
                    targetMapping.setPath(splitMappingString[1]);
                    targetMapping.setPropertyDefinition(toscaElement.getProperties().get(targetMapping.getProperty()));
                    PropertySubMapping propertySubMapping = new PropertySubMapping(sourceMapping, targetMapping);
                    propertyMapping.getSubMappings().add(propertySubMapping);
                    propertyMappingsL.add(propertyMapping);
                }
            }

            propertyMappings.put(key[0], propertyMappingsL);
        }

        return propertyMappings;
    }

    /**
     * Extract the property name and sub-path from a string formatted as propname.subpath
     *
     * @param fullPath
     *            The string formatted as propname.subpath.
     * @return An array that contains [propname, subpath] where subpath may be null in case there is no subpath.
     */
    private static String[] asPropAndSubPath(String fullPath) {
        int index = fullPath.indexOf(".");
        if (index < 1 || index == fullPath.length()) {
            return new String[] { fullPath, null };
        }
        return new String[] { fullPath.substring(0, index), fullPath.substring(index + 1) };
    }
}