package alien4cloud.paas.cloudify3.util.mapping;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;
import org.alien4cloud.tosca.model.definitions.ListPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import alien4cloud.paas.cloudify3.error.PropertyValueMappingException;
import alien4cloud.utils.services.PropertyValueService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;

/**
 * Simple utility that deep
 */
@Slf4j
@SuppressWarnings({ "unchecked", "rawtypes" })
public final class PropertyValueUtil {

    /**
     * Map properties of a template from tosca to cloudify properties.
     *
     * @param allTypesPropertiesMappings
     *            Properties mapping for all tosca element in the topology
     * @param nodeType
     *            Tosca elementType of the template for which to map properties
     * @param properties
     *            The properties to map
     * @return A map of mapped properties
     */
    public static Map<String, AbstractPropertyValue> mapProperties(Map<String, Map<String, List<IPropertyMapping>>> allTypesPropertiesMappings, String nodeType,
            Map<String, AbstractPropertyValue> properties) {

        if (allTypesPropertiesMappings == null || allTypesPropertiesMappings.isEmpty()) {
            // do not change prop map
            return properties;
        }

        // first of all get the mapping for the node type
        Map<String, List<IPropertyMapping>> typePropertiesMappings = allTypesPropertiesMappings.get(nodeType);
        return mapProperties(allTypesPropertiesMappings, typePropertiesMappings, properties);
    }

    /**
     * Map properties of a template from tosca to cloudify properties.
     *
     * @param typePropertiesMappings
     *            Properties mapping for the tosca element related to the template
     * @param properties
     *            The properties to map
     * @return A map of mapped properties
     */
    public static Map<String, AbstractPropertyValue> mapProperties(Map<String, List<IPropertyMapping>> typePropertiesMappings,
            Map<String, AbstractPropertyValue> properties) {
        Map<String, Map<String, List<IPropertyMapping>>> allTypesPropertiesMappings = Maps.newLinkedHashMap();
        return mapProperties(allTypesPropertiesMappings, typePropertiesMappings, properties);
    }

    /**
     * Map properties of a template from tosca to cloudify properties.
     *
     * @param allTypesPropertiesMappings
     *            Properties mapping for all tosca element in the topology
     * @param typePropertiesMappings
     *            Properties mapping for the tosca element related to the template
     * @param properties
     *            The properties to map
     * @return A map of mapped properties
     */
    private static Map<String, AbstractPropertyValue> mapProperties(Map<String, Map<String, List<IPropertyMapping>>> allTypesPropertiesMappings,
            Map<String, List<IPropertyMapping>> typePropertiesMappings, Map<String, AbstractPropertyValue> properties) {
        if (typePropertiesMappings == null || typePropertiesMappings.isEmpty()) {
            // do not change prop map
            return properties;
        }

        Map<String, AbstractPropertyValue> mappedProperties = Maps.newLinkedHashMap();
        for (Map.Entry<String, AbstractPropertyValue> propertyEntry : properties.entrySet()) {
            PropertyValue sourcePropertyValue = (PropertyValue) propertyEntry.getValue();
            if (sourcePropertyValue == null) {
                continue;
            }
            mapProperty(allTypesPropertiesMappings, typePropertiesMappings, propertyEntry.getKey(), sourcePropertyValue, mappedProperties);
        }
        return mappedProperties;
    }

    /**
     * Check the type of the mapping and process it
     * 
     * @param allTypesPropertiesMappings
     * @param typesPropertiesMappings
     * @param propertyName
     * @param sourcePropertyValue
     * @param mappedProperties
     */
    private static void mapProperty(Map<String, Map<String, List<IPropertyMapping>>> allTypesPropertiesMappings,
            Map<String, List<IPropertyMapping>> typesPropertiesMappings, String propertyName, PropertyValue sourcePropertyValue,
            Map<String, AbstractPropertyValue> mappedProperties) {

        List<IPropertyMapping> mappingL = typesPropertiesMappings.get(propertyName);
        if (mappingL != null) {
            for (IPropertyMapping mapping : mappingL) {
                if (mapping == null) {
                    // if the property is not mapped, just keep it as is.
                    mergeAndAddMappedProperty(propertyName, sourcePropertyValue, mappedProperties);
                } else if (mapping instanceof PropertyMapping) {
                    processPropertyMapping(allTypesPropertiesMappings, (PropertyMapping) mapping, propertyName, sourcePropertyValue, mappedProperties);
                } else if (mapping instanceof ComplexPropertyMapping) {
                    processComplexPropertyMapping(allTypesPropertiesMappings, propertyName, sourcePropertyValue, mappedProperties, mapping);
                }
            }
        } else {
            mergeAndAddMappedProperty(propertyName, sourcePropertyValue, mappedProperties);
        }
    }

    /**
     * process a {@link ComplexPropertyMapping}. This is a mapping referring to a data type linked to a complex property (entry_schema)
     * 
     * @param allTypesPropertiesMappings
     * @param propertyName
     * @param sourcePropertyValue
     * @param mappedProperties
     * @param mapping
     */
    private static void processComplexPropertyMapping(Map<String, Map<String, List<IPropertyMapping>>> allTypesPropertiesMappings, String propertyName,
            PropertyValue sourcePropertyValue, Map<String, AbstractPropertyValue> mappedProperties, IPropertyMapping mapping) {
        ComplexPropertyMapping complexPropertyMapping = (ComplexPropertyMapping) mapping;
        String complexPropertyType = complexPropertyMapping.getType();
        if (complexPropertyType == null) {
            log.warn(String.format("The property '%s' is known as a complex property mapping but the type isn't defined, ignore the mapping !", propertyName));
            return;
        }
        // get the mapping for this type
        Map<String, List<IPropertyMapping>> typeMapping = allTypesPropertiesMappings.get(complexPropertyType);
        if (typeMapping == null) {
            log.warn(String.format(
                    "The property '%s' is known as a complex property mapping but the mapping for the type '%s' can not be found, ignore the mapping !",
                    propertyName, complexPropertyType));
            return;
        }
        PropertyValue mappingResult = null;

        // check and process according to the type of the mapping
        if (complexPropertyMapping.isList()) {
            // the mapping concerns a list
            if (!(sourcePropertyValue instanceof ListPropertyValue)) {
                // the property value is not a list, ignore and log
                log.warn(String.format("The property '%s' should be a list but is not, ignore the mapping !", propertyName));
                return;
            }
            mappingResult = processListPropertyMapping(allTypesPropertiesMappings, propertyName, sourcePropertyValue, typeMapping);
        } else {
            // the mapping concerns a complex type other than a list (let's say a map)
            if (!(sourcePropertyValue instanceof ComplexPropertyValue)) {
                // the item is not a complex property, ignore and log
                log.warn(String.format("The property '%s' should be a complex property but is not, ignore the mapping !", propertyName));
                return;
            }
            mappingResult = processMapPropertyMapping(allTypesPropertiesMappings, sourcePropertyValue, typeMapping);
        }

        // then, we process the simple mapping registered for this property
        // this allows for example to change the property name on the target
        if (complexPropertyMapping.getRelatedSimplePropertiesMapping() != null) {
            for (IPropertyMapping propertyMapping : complexPropertyMapping.getRelatedSimplePropertiesMapping()) {
                processPropertyMapping(allTypesPropertiesMappings, (PropertyMapping) propertyMapping, propertyName, mappingResult, mappedProperties);
            }
        } else {
            // if the property is not mapped, just keep it as is.
            mergeAndAddMappedProperty(propertyName, mappingResult, mappedProperties);
        }
    }

    /**
     * process the case the source property value is a complex type (a map)
     * 
     * @param allTypesPropertiesMappings
     * @param sourcePropertyValue
     * @param typeMapping
     * @return
     */
    private static PropertyValue processMapPropertyMapping(Map<String, Map<String, List<IPropertyMapping>>> allTypesPropertiesMappings,
            PropertyValue sourcePropertyValue, Map<String, List<IPropertyMapping>> typeMapping) {
        ComplexPropertyValue complexPropertyValue = (ComplexPropertyValue) sourcePropertyValue;
        Map<String, AbstractPropertyValue> itemProperties = Maps.newLinkedHashMap();
        for (Entry<String, Object> complexPropertyEntry : complexPropertyValue.getValue().entrySet()) {
            PropertyValue complexPropertyEntryValue = propertyValueFromObject(complexPropertyEntry.getValue());
            String complexPropertyEntryName = complexPropertyEntry.getKey();
            mapProperty(allTypesPropertiesMappings, typeMapping, complexPropertyEntryName, complexPropertyEntryValue, itemProperties);
        }
        return propertyValueFromObject(itemProperties);
    }

    /**
     * Process the case the source property value is a list
     * 
     * @param allTypesPropertiesMappings
     * @param propertyName
     * @param sourcePropertyValue
     * @param typeMapping
     * @return
     */
    private static PropertyValue processListPropertyMapping(Map<String, Map<String, List<IPropertyMapping>>> allTypesPropertiesMappings, String propertyName,
            PropertyValue sourcePropertyValue, Map<String, List<IPropertyMapping>> typeMapping) {
        ListPropertyValue listPropertyValue = (ListPropertyValue) sourcePropertyValue;
        ListPropertyValue targetListPropertyValue = new ListPropertyValue(new ArrayList<Object>());
        for (Object listItem : listPropertyValue.getValue()) {
            if (!(listItem instanceof ComplexPropertyValue)) {
                // the item is not a complex property, ignore and log
                log.warn(String.format("The property '%s' list item should be a complex property but is not, ignore the mapping !", propertyName));
                continue;
            }
            ComplexPropertyValue complexPropertyValue = (ComplexPropertyValue) listItem;
            // for each item we build a map
            Map<String, AbstractPropertyValue> itemProperties = Maps.newLinkedHashMap();
            for (Entry<String, Object> complexPropertyEntry : complexPropertyValue.getValue().entrySet()) {
                PropertyValue complexPropertyEntryValue = propertyValueFromObject(complexPropertyEntry.getValue());
                String complexPropertyEntryName = complexPropertyEntry.getKey();
                mapProperty(allTypesPropertiesMappings, typeMapping, complexPropertyEntryName, complexPropertyEntryValue, itemProperties);
            }
            targetListPropertyValue.getValue().add(itemProperties);
        }
        return targetListPropertyValue;
    }

    /**
     * process the case the source property value is a simple type
     * 
     * @param propertyMappings
     * @param mapping
     * @param propertyName
     * @param sourcePropertyValue
     * @param mappedProperties
     */
    private static void processPropertyMapping(Map<String, Map<String, List<IPropertyMapping>>> propertyMappings, PropertyMapping mapping, String propertyName,
            PropertyValue sourcePropertyValue, Map<String, AbstractPropertyValue> mappedProperties) {

        if (mapping == null || mapping.getSubMappings().size() == 0) {
            // if the property is not mapped, just keep it as is.
            mergeAndAddMappedProperty(propertyName, sourcePropertyValue, mappedProperties);
        } else {
            // if the property is mapped then apply the mapping.
            for (PropertySubMapping subMapping : mapping.getSubMappings()) {
                String sourcePath = subMapping.getSourceMapping().getPath();
                TargetMapping targetMapping = subMapping.getTargetMapping();

                if (targetMapping.getProperty() == null) {
                    continue; // skip this property
                }

                Object sourceValue = PropertyValueService.getValue(sourcePropertyValue.getValue(), sourcePath);
                // if there is a specified unit, convert the value to the expected unit.
                if (targetMapping.getUnit() != null) {
                    // need the property type to IComparablePropertyType
                    sourceValue = PropertyValueService.getValueInUnit(sourceValue, targetMapping.getUnit(), targetMapping.isCeil(),
                            subMapping.getSourceMapping().getPropertyDefinition());
                }

                if (targetMapping.getPath() == null) {
                    // set the property with the value
                    mergeAndAddMappedProperty(targetMapping.getProperty(), propertyValueFromObject(sourceValue), mappedProperties);
                } else {
                    // extract the value
                    PropertyValue targetProperty = (PropertyValue) mappedProperties.get(targetMapping.getProperty());
                    Object targetValue = sourceValue;
                    if (targetProperty != null) {
                        targetValue = PropertyValueService.getValue(targetProperty.getValue(), targetMapping.getPath());
                        targetValue = merge(sourceValue, targetValue);
                    } else {
                        targetProperty = propertyValueFromObject(new HashMap<>());
                        mappedProperties.put(targetMapping.getProperty(), targetProperty);
                    }
                    // set the value
                    setValueInMap(targetProperty.getValue(), targetMapping.getPath(), targetValue);
                }
            }
        }
    }

    private static void mergeAndAddMappedProperty(String propertyName, PropertyValue sourcePropertyValue, Map<String, AbstractPropertyValue> mappedProperties) {
        PropertyValue mappedProperty = (PropertyValue) mappedProperties.get(propertyName);
        if (sourcePropertyValue.getValue() != null) {
            mappedProperty = merge(sourcePropertyValue, mappedProperty);
        }
        if (mappedProperty != null) {
            mappedProperties.put(propertyName, mappedProperty);
        }
    }

    /**
     * Return a property value based on the type of an object.
     *
     * @param object
     *            The object to wrap into a PropertyValue.
     * @return A property value that wraps the given object.
     */
    private static PropertyValue propertyValueFromObject(Object object) {
        if (object instanceof PropertyValue) {
            return (PropertyValue) object;
        } else if (object instanceof Map) {
            return new ComplexPropertyValue((Map<String, Object>) object);
        } else if (object instanceof List) {
            return new ListPropertyValue((List<Object>) object);
        } else {
            return new ScalarPropertyValue((String) object);
        }
    }

    /**
     * Merge a source property value into a target property. If target is not null, the source is merged directly in the target object and the target object is
     * returned.
     *
     * @param source
     *            The source property value.
     * @param target
     *            The target property value.
     * @return The merged object.
     */
    public static PropertyValue merge(PropertyValue source, PropertyValue target) {
        if (source == null) {
            return target;
        }
        if (target == null) {
            return deepClone(source);
        }
        // perform the property merge.
        Object clonedSource = deepClone(source.getValue());
        // merge the clonedSource into the target value
        target.setValue(merge(clonedSource, target.getValue()));
        return target;
    }

    private static Object merge(Object sourcePropertyValueObject, Object targetPropertyValueObject) {
        if (targetPropertyValueObject == null) {
            return sourcePropertyValueObject;
        } else if (sourcePropertyValueObject == null) {
            return targetPropertyValueObject;
        } else if (sourcePropertyValueObject instanceof Map && targetPropertyValueObject instanceof Map) {
            Map result = Maps.newLinkedHashMap((Map) targetPropertyValueObject);
            result.putAll((Map) sourcePropertyValueObject);
            return result;
        } else {
            return sourcePropertyValueObject;
        }
    }

    private static Object getOrInit(Map map, String element) {
        Object value = map.get(element);
        if (value == null) {
            value = new HashMap<>();
            map.put(element, value);
        }
        return value;
    }

    private static void setValueInMap(Object target, String path, Object value) {
        if (target instanceof Map) {
            Map current = (Map) target;
            String currentPath = path;
            String[] pathElements = currentPath.split("\\.");
            if (pathElements.length > 1) {
                currentPath = currentPath.substring(pathElements[0].length() + 1);
                Object midwayValue = getOrInit(current, pathElements[0]);
                setValueInMap(midwayValue, currentPath, value);
            } else {
                current.put(currentPath, value);
            }
            return;
        }
        throw new PropertyValueMappingException("Expected a map");
    }

    /**
     * Simple utility that deep clones an object made of Map, List and String.
     *
     * @param propertyValue
     *            The property value to clone.
     */
    public static PropertyValue deepClone(PropertyValue propertyValue) {
        if (propertyValue instanceof ListPropertyValue) {
            return new ListPropertyValue(deepClone(((ListPropertyValue) propertyValue).getValue()));
        } else if (propertyValue instanceof ComplexPropertyValue) {
            return new ComplexPropertyValue(deepClone(((ComplexPropertyValue) propertyValue).getValue()));
        }
        // must be a scalar property value
        return new ScalarPropertyValue(((ScalarPropertyValue) propertyValue).getValue());
    }

    /**
     * Deep clone an object that compose a property value (every element is a String, a Map or a List).
     *
     * @param propertyValueObject
     *            The object to clone.
     * @return A clone of the property value object.
     */
    private static <T> T deepClone(T propertyValueObject) {
        T clone;
        if (propertyValueObject instanceof Map) {
            Map map = Maps.newLinkedHashMap();
            Map<String, ?> propertyValueObjectMap = (Map) propertyValueObject;
            for (Map.Entry<String, ?> entry : propertyValueObjectMap.entrySet()) {
                // key is a string in property values structure
                map.put(entry.getKey(), deepClone(entry.getValue()));
            }
            clone = (T) map;
        } else if (propertyValueObject instanceof List) {
            List list = Lists.newArrayList();
            for (Object element : ((List) propertyValueObject)) {
                list.add(deepClone(element));
            }
            clone = (T) list;
        } else if (propertyValueObject instanceof String) {
            clone = propertyValueObject;
        } else {
            log.warn("Property deep clone is just making a simple copy for type {} and value {}. Expecting a String for these situations.",
                    propertyValueObject.getClass().getName(), propertyValueObject);
            clone = propertyValueObject;
        }

        return clone;
    }
}