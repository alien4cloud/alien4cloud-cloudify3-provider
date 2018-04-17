package alien4cloud.paas.cloudify3.blueprint;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import alien4cloud.paas.cloudify3.configuration.MappingConfiguration;
import alien4cloud.paas.cloudify3.model.DeploymentPropertiesNames;
import alien4cloud.paas.cloudify3.service.OrchestratorDeploymentPropertiesService;
import alien4cloud.paas.cloudify3.service.PropertyEvaluatorService;
import alien4cloud.paas.cloudify3.service.model.CloudifyDeployment;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.services.PropertyValueService;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.alien4cloud.tosca.normative.types.IPropertyType;
import org.alien4cloud.tosca.normative.types.ScalarType;
import org.alien4cloud.tosca.normative.types.ToscaTypes;
import org.alien4cloud.tosca.utils.ToscaTypeUtils;
import org.apache.commons.lang3.StringUtils;

import static alien4cloud.utils.AlienUtils.safe;

public class CommonGenerationUtil extends AbstractGenerationUtil {

    private OrchestratorDeploymentPropertiesService deploymentPropertiesService;

    public CommonGenerationUtil(MappingConfiguration mappingConfiguration, CloudifyDeployment alienDeployment, Path recipePath,
            PropertyEvaluatorService propertyEvaluatorService, OrchestratorDeploymentPropertiesService deploymentPropertiesService) {
        super(mappingConfiguration, alienDeployment, recipePath, propertyEvaluatorService);

        this.deploymentPropertiesService = deploymentPropertiesService;
    }

    public String getMonitoringInterval() {
        return deploymentPropertiesService.getValueOrDefault(alienDeployment.getProviderDeploymentProperties(),
                DeploymentPropertiesNames.MONITORING_INTERVAL_INMINUTE);
    }

    public String getScalarPropertyValue(NodeTemplate nodeTemplate, String propertyName) {
        AbstractPropertyValue value = (AbstractPropertyValue) MapUtil.get(nodeTemplate.getProperties(), propertyName);
        if (value != null) {
            return ((ScalarPropertyValue) value).getValue();
        }

        return "";
    }

    public String truncateString(String toBeAbbreviated, int maxWidth) {
        if (StringUtils.isEmpty(toBeAbbreviated)) {
            return toBeAbbreviated;
        } else {
            if (toBeAbbreviated.length() > maxWidth) {
                return toBeAbbreviated.substring(0, maxWidth);
            } else {
                return toBeAbbreviated;
            }
        }
    }

    public boolean isFromType(String type, AbstractInheritableToscaType indexedInheritableToscaElement) {
        return ToscaTypeUtils.isOfType(indexedInheritableToscaElement, type);
    }

    public boolean doesVelocityFileExists(String velocityFilePath) {
        if (Files.exists(Paths.get(velocityFilePath))) {
            return true;
        }
        return false;
    }

    public String getValueInUnit(PaaSNodeTemplate template, String propertyName, String unit, boolean ceil) {
        if (!template.getIndexedToscaElement().getProperties().containsKey(propertyName)) {
            throw new IllegalArgumentException(String.format("Unknown property '%s' in node template '%s'", propertyName, template.getId()));
        }
        PropertyDefinition propertyDefinition = template.getIndexedToscaElement().getProperties().get(propertyName);
        IPropertyType type = ToscaTypes.fromYamlTypeName(propertyDefinition.getType());
        if (type instanceof ScalarType) {
            AbstractPropertyValue apv = template.getTemplate().getProperties().get(propertyName);
            if (apv instanceof PropertyValue) {
                return PropertyValueService.getValueInUnit(((PropertyValue) apv).getValue(), unit, ceil, propertyDefinition);
            } else {
                throw new IllegalArgumentException(
                        String.format("Property '%s' in node template '%s' is not a property value", propertyName, template.getId()));
            }
        }
        throw new IllegalArgumentException(String.format("Property '%s' in node template '%s' is not a scalar unit type", propertyName, template.getId()));
    }

    public Set<Entry> safeEntrySet(Map map) {
        return safe(map).entrySet();
    }

    public boolean isNull(Object o) {
        return o == null;
    }
}
