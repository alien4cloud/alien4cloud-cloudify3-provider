package alien4cloud.paas.cloudify3.blueprint;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.alien4cloud.tosca.model.types.NodeType;
import alien4cloud.paas.cloudify3.configuration.MappingConfiguration;
import alien4cloud.paas.cloudify3.service.PropertyEvaluatorService;
import alien4cloud.paas.cloudify3.service.model.CloudifyDeployment;

@AllArgsConstructor
public abstract class AbstractGenerationUtil {

    protected MappingConfiguration mappingConfiguration;

    @Getter
    protected CloudifyDeployment alienDeployment;

    protected Path recipePath;

    protected PropertyEvaluatorService propertyEvaluatorService;

    protected boolean typeMustBeMappedToCloudifyType(String toscaType) {
        return mappingConfiguration.getNormativeTypes().containsKey(toscaType);
    }

    protected NodeType getTypeFromName(String name, List<NodeType> types) {
        for (NodeType type : types) {
            if (name.equals(type.getId())) {
                return type;
            }
        }
        return null;
    }

    public boolean collectionHasElement(Collection<?> list) {
        return list != null && !list.isEmpty();
    }

    public boolean mapHasEntries(Map<?, ?> map) {
        return map != null && !map.isEmpty();
    }

    public String tryToMapToCloudifyType(String toscaType) {
        String mappedType = mappingConfiguration.getNormativeTypes().get(toscaType);
        return mappedType != null ? mappedType : toscaType;
    }

}
