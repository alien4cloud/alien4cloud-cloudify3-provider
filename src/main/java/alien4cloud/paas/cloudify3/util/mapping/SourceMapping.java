package alien4cloud.paas.cloudify3.util.mapping;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SourceMapping {
    String path; // optional sub-path from the target property
    PropertyDefinition propertyDefinition; // the property definition that matches the source property.
}
