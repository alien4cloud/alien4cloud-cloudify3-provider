package alien4cloud.paas.cloudify3.blueprint;

import java.nio.file.Path;

import alien4cloud.orchestrators.locations.services.LocationResourceTypes;
import alien4cloud.paas.cloudify3.configuration.MappingConfiguration;
import alien4cloud.paas.cloudify3.service.ArtifactRegistryService;
import alien4cloud.paas.cloudify3.service.OrchestratorDeploymentPropertiesService;
import alien4cloud.paas.cloudify3.service.PropertyEvaluatorService;
import alien4cloud.paas.cloudify3.service.model.CloudifyDeployment;
import alien4cloud.tosca.serializer.ToscaPropertySerializerUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Some utilities method which help generating Cloudify 3 blueprint
 *
 * @author Minh Khang VU
 */
@Slf4j
@Getter
public class BlueprintGenerationUtil extends AbstractGenerationUtil {

    private NonNativeTypeGenerationUtil nonNative;

    private NativeTypeGenerationUtil natives;

    private WorkflowGenerationUtil workflow;

    private CommonGenerationUtil common;

    private NetworkGenerationUtil network;

    private ToscaPropertySerializerUtils property;

    public BlueprintGenerationUtil(MappingConfiguration mappingConfiguration, CloudifyDeployment alienDeployment, Path recipePath,
            PropertyEvaluatorService propertyEvaluatorService, OrchestratorDeploymentPropertiesService deploymentPropertiesService,
            ArtifactRegistryService artifactRegistryService) {
        super(mappingConfiguration, alienDeployment, recipePath, propertyEvaluatorService);
        this.nonNative = new NonNativeTypeGenerationUtil(mappingConfiguration, alienDeployment, recipePath, propertyEvaluatorService, artifactRegistryService);
        this.workflow = new WorkflowGenerationUtil(mappingConfiguration, alienDeployment, recipePath, propertyEvaluatorService);
        this.common = new CommonGenerationUtil(mappingConfiguration, alienDeployment, recipePath, propertyEvaluatorService, deploymentPropertiesService);
        this.network = new NetworkGenerationUtil(mappingConfiguration, alienDeployment, recipePath, propertyEvaluatorService);
        this.natives = new NativeTypeGenerationUtil(mappingConfiguration, alienDeployment, recipePath, propertyEvaluatorService);
        this.property = new ToscaPropertySerializerUtils();
    }
}
