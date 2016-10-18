package alien4cloud.paas.cloudify3.util.mapping;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;

/**
 * Mapping information for the target.
 */
@Getter
@Setter
public class TargetMapping {
    String property; // name of the property in which to map the value
    String path; // optional sub-path from the target property
    String unit; // optional parameter that allows conversion of a Scalar Unit to Scalar based on the required unit.
    boolean ceil; // optional parameter that round up to the nearest integer the value after conversion.
    PropertyDefinition propertyDefinition; // the property definition that matches the target property.
}