package alien4cloud.paas.cloudify3.service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.elasticsearch.common.collect.Sets;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import alien4cloud.component.repository.ArtifactLocalRepository;
import alien4cloud.component.repository.ArtifactRepositoryConstants;
import alien4cloud.component.repository.CsarFileRepository;
import alien4cloud.component.repository.exception.CSARVersionNotFoundException;
import alien4cloud.model.components.*;
import alien4cloud.model.topology.AbstractTemplate;
import alien4cloud.model.topology.Capability;
import alien4cloud.paas.IPaaSTemplate;
import alien4cloud.paas.cloudify3.blueprint.BlueprintGenerationUtil;
import alien4cloud.paas.cloudify3.configuration.CloudConfigurationHolder;
import alien4cloud.paas.cloudify3.configuration.MappingConfigurationHolder;
import alien4cloud.paas.cloudify3.service.model.CloudifyDeployment;
import alien4cloud.paas.cloudify3.service.model.OperationWrapper;
import alien4cloud.paas.cloudify3.service.model.Relationship;
import alien4cloud.paas.cloudify3.util.VelocityUtil;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;
import alien4cloud.plugin.model.ManagedPlugin;
import alien4cloud.tosca.ToscaUtils;
import alien4cloud.utils.FileUtil;
import alien4cloud.utils.MapUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Handle blueprint generation from alien model
 *
 * @author Minh Khang VU
 */
@Component("cloudify-blueprint-service")
@Slf4j
public class BlueprintService {

    public static final String TOSCA_NODES_CONTAINER_APPLICATION_DOCKER_CONTAINER = "tosca.nodes.Container.Application.DockerContainer";
    public static final String ALIEN_CAPABILITIES_ENDPOINT_DOCKER = "alien.capabilities.endpoint.Docker";

    @Resource
    private CloudConfigurationHolder cloudConfigurationHolder;

    @Resource
    private MappingConfigurationHolder mappingConfigurationHolder;

    @Resource
    private CsarFileRepository repository;

    @Resource
    private ArtifactLocalRepository artifactRepository;

    @Resource
    private PropertyEvaluatorService propertyEvaluatorService;

    @Resource
    private OrchestratorDeploymentPropertiesService deploymentPropertiesService;

    @Resource
    private ManagedPlugin pluginContext;

    private Path recipeDirectoryPath;

    private Path pluginRecipeResourcesPath;

    private Set<BlueprintGeneratorExtension> blueprintGeneratorExtensions;

    public synchronized void addBlueprintGeneratorExtension(BlueprintGeneratorExtension blueprintGeneratorExtension) {
        if (blueprintGeneratorExtensions == null) {
            blueprintGeneratorExtensions = Sets.newLinkedHashSet();
        }
        blueprintGeneratorExtensions.add(blueprintGeneratorExtension);
    }

    public synchronized void removeBlueprintGeneratorExtension(BlueprintGeneratorExtension blueprintGeneratorExtension) {
        blueprintGeneratorExtensions.remove(blueprintGeneratorExtension);
    }

    @PostConstruct
    public void postConstruct() throws IOException {
        synchronized (BlueprintService.class) {
            this.pluginRecipeResourcesPath = this.pluginContext.getPluginPath().resolve("recipe");
            // if (Files.exists(this.pluginRecipeResourcesPath.resolve("velocity").resolve("provider"))) {
            // return;
            // }
            log.info("Copy provider templates to velocity main template's folder");
            // This is a workaround to copy provider templates to velocity folder as relative path do not work with velocity
            List<Path> providerTemplates = FileUtil.listFiles(this.pluginContext.getPluginPath().resolve("provider"), ".+\\.yaml\\.vm");
            for (Path providerTemplate : providerTemplates) {
                String relativizedPath = FileUtil.relativizePath(this.pluginContext.getPluginPath(), providerTemplate);
                Path providerTemplateTargetPath = this.pluginRecipeResourcesPath.resolve("velocity").resolve(relativizedPath);
                Files.createDirectories(providerTemplateTargetPath.getParent());
                Files.copy(providerTemplate, providerTemplateTargetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /**
     * Delete a blueprint on the file system.
     *
     * @param deploymentPaaSId
     *            Alien's paas deployment id used to identify the blueprint.
     */
    public void deleteBlueprint(String deploymentPaaSId) {
        try {
            FileUtil.delete(resolveBlueprintPath(deploymentPaaSId));
        } catch (IOException e) {
            log.warn("Unable to delete generated blueprint for recipe " + deploymentPaaSId, e);
        }
    }

    /**
     * Generate blueprint from an alien deployment request
     *
     * @param alienDeployment
     *            the alien deployment's configuration
     * @return the generated blueprint
     */
    public Path generateBlueprint(CloudifyDeployment alienDeployment) throws IOException, CSARVersionNotFoundException {
        // Where the whole blueprint will be generated
        Path generatedBlueprintDirectoryPath = resolveBlueprintPath(alienDeployment.getDeploymentPaaSId());
        if (Files.exists(generatedBlueprintDirectoryPath)) {
            deleteBlueprint(alienDeployment.getDeploymentPaaSId());
        }

        // Where the main blueprint file will be generated
        Path generatedBlueprintFilePath = generatedBlueprintDirectoryPath.resolve("blueprint.yaml");
        BlueprintGenerationUtil util = new BlueprintGenerationUtil(mappingConfigurationHolder.getMappingConfiguration(), alienDeployment,
                generatedBlueprintDirectoryPath, propertyEvaluatorService, deploymentPropertiesService);

        // The velocity context will be filed up with information in order to be able to generate deployment
        Map<String, Object> context = Maps.newHashMap();
        context.put("cloud", cloudConfigurationHolder.getConfiguration());
        context.put("mapping", mappingConfigurationHolder.getMappingConfiguration());
        context.put("util", util);
        context.put("deployment", alienDeployment);
        context.put("newline", "\n");

        // Copy artifacts
        for (PaaSNodeTemplate nonNative : alienDeployment.getNonNatives()) {
            IndexedNodeType nonNativeType = nonNative.getIndexedToscaElement();
            // Don't process a node more than once
            copyDeploymentArtifacts(generatedBlueprintDirectoryPath, nonNative.getId(), nonNative.getNodeTemplate(), nonNativeType);
            copyImplementationArtifacts(generatedBlueprintDirectoryPath, nonNative.getId(), nonNative.getInterfaces());
            List<PaaSRelationshipTemplate> relationships = nonNative.getRelationshipTemplates();
            for (PaaSRelationshipTemplate relationship : relationships) {
                if (relationship.getSource().equals(nonNative.getId())) {
                    IndexedRelationshipType relationshipType = relationship.getIndexedToscaElement();
                    copyDeploymentArtifacts(generatedBlueprintDirectoryPath, nonNative.getId(), relationship.getRelationshipTemplate(), relationshipType);
                    copyImplementationArtifacts(generatedBlueprintDirectoryPath, nonNative.getId(), relationship.getInterfaces());
                }
            }
        }

        // Wrap all implementation script into a wrapper so it can be called from cloudify 3 with respect of TOSCA.
        for (PaaSNodeTemplate node : alienDeployment.getNonNatives()) {
            Map<String, Interface> interfaces = util.getNonNative().getNodeInterfaces(node);
            if (MapUtils.isNotEmpty(interfaces)) {
                for (Map.Entry<String, Interface> inter : interfaces.entrySet()) {
                    Map<String, Operation> operations = inter.getValue().getOperations();
                    for (Map.Entry<String, Operation> operationEntry : operations.entrySet()) {
                        Map<String, Map<String, DeploymentArtifact>> artifacts = Maps.newLinkedHashMap();
                        // Special case when it's a node operation, then the only artifacts that are being injected is of the node it-self
                        if (MapUtils.isNotEmpty(node.getIndexedToscaElement().getArtifacts())) {
                            artifacts.put(node.getId(), node.getIndexedToscaElement().getArtifacts());
                        }
                        generateOperationScriptWrapper(inter.getKey(), operationEntry.getKey(), operationEntry.getValue(), node, util, context,
                                generatedBlueprintDirectoryPath, artifacts, null, alienDeployment.getAllNodes());
                    }
                }
            }
            List<PaaSRelationshipTemplate> relationships = util.getNonNative().getSourceRelationships(node);
            for (PaaSRelationshipTemplate relationship : relationships) {
                Map<String, Interface> relationshipInterfaces = util.getNonNative().getRelationshipInterfaces(relationship);
                if (MapUtils.isNotEmpty(relationshipInterfaces)) {
                    for (Map.Entry<String, Interface> inter : relationshipInterfaces.entrySet()) {
                        Map<String, Operation> operations = inter.getValue().getOperations();
                        for (Map.Entry<String, Operation> operationEntry : operations.entrySet()) {
                            Relationship keyRelationship = new Relationship(relationship.getId(), relationship.getSource(),
                                    relationship.getRelationshipTemplate().getTarget());
                            Map<Relationship, Map<String, DeploymentArtifact>> relationshipArtifacts = Maps.newLinkedHashMap();
                            if (MapUtils.isNotEmpty(relationship.getIndexedToscaElement().getArtifacts())) {
                                relationshipArtifacts.put(keyRelationship, relationship.getIndexedToscaElement().getArtifacts());
                            }
                            Map<String, Map<String, DeploymentArtifact>> artifacts = Maps.newLinkedHashMap();
                            Map<String, DeploymentArtifact> sourceArtifacts = alienDeployment.getAllNodes().get(relationship.getSource())
                                    .getIndexedToscaElement().getArtifacts();
                            if (MapUtils.isNotEmpty(sourceArtifacts)) {
                                artifacts.put(relationship.getSource(), sourceArtifacts);
                            }
                            Map<String, DeploymentArtifact> targetArtifacts = alienDeployment.getAllNodes()
                                    .get(relationship.getRelationshipTemplate().getTarget()).getIndexedToscaElement().getArtifacts();
                            if (MapUtils.isNotEmpty(targetArtifacts)) {
                                artifacts.put(relationship.getRelationshipTemplate().getTarget(), targetArtifacts);
                            }
                            generateOperationScriptWrapper(inter.getKey(), operationEntry.getKey(), operationEntry.getValue(), relationship, util, context,
                                    generatedBlueprintDirectoryPath, artifacts, relationshipArtifacts, alienDeployment.getAllNodes());
                        }
                    }
                }
            }
        }

        if (!alienDeployment.getNonNatives().isEmpty()) {
            Files.copy(pluginRecipeResourcesPath.resolve("wrapper/scriptWrapper.sh"), generatedBlueprintDirectoryPath.resolve("scriptWrapper.sh"));
            Files.copy(pluginRecipeResourcesPath.resolve("wrapper/scriptWrapper.bat"), generatedBlueprintDirectoryPath.resolve("scriptWrapper.bat"));
        }

        // custom workflows section
        Path wfPluginDir = generatedBlueprintDirectoryPath.resolve("plugins/custom_wf_plugin/plugin");
        Files.createDirectories(wfPluginDir);
        Files.copy(pluginRecipeResourcesPath.resolve("custom_wf_plugin/setup.py"),
                generatedBlueprintDirectoryPath.resolve("plugins/custom_wf_plugin/setup.py"));
        Files.copy(pluginRecipeResourcesPath.resolve("custom_wf_plugin/plugin/__init__.py"),
                generatedBlueprintDirectoryPath.resolve("plugins/custom_wf_plugin/plugin/__init__.py"));
        Files.copy(pluginRecipeResourcesPath.resolve("custom_wf_plugin/plugin/handlers.py"),
                generatedBlueprintDirectoryPath.resolve("plugins/custom_wf_plugin/plugin/handlers.py"));
        Files.copy(pluginRecipeResourcesPath.resolve("custom_wf_plugin/plugin/utils.py"),
                generatedBlueprintDirectoryPath.resolve("plugins/custom_wf_plugin/plugin/utils.py"));
        Files.copy(pluginRecipeResourcesPath.resolve("custom_wf_plugin/plugin/workflow.py"),
                generatedBlueprintDirectoryPath.resolve("plugins/custom_wf_plugin/plugin/workflow.py"));
        VelocityUtil.generate(pluginRecipeResourcesPath.resolve("custom_wf_plugin/plugin/workflows.py.vm"),
                generatedBlueprintDirectoryPath.resolve("plugins/custom_wf_plugin/plugin/workflows.py"), context);
        FileUtil.zip(generatedBlueprintDirectoryPath.resolve("plugins/custom_wf_plugin"),
                generatedBlueprintDirectoryPath.resolve("plugins/custom_wf_plugin.zip"));

        // device
        FileUtil.copy(pluginRecipeResourcesPath.resolve("device-mapping-scripts"), generatedBlueprintDirectoryPath.resolve("device-mapping-scripts"),
                StandardCopyOption.REPLACE_EXISTING);
        VelocityUtil.generate(pluginRecipeResourcesPath.resolve("velocity/mapping.py.vm"),
                generatedBlueprintDirectoryPath.resolve("device-mapping-scripts/mapping.py"), context);

        // monitor
        if (CollectionUtils.isNotEmpty(alienDeployment.getNodesToMonitor())) {
            FileUtil.copy(pluginRecipeResourcesPath.resolve("monitor"), generatedBlueprintDirectoryPath.resolve("monitor"),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        // custom openstack plugin (scalable compute workaround)
        Files.copy(pluginRecipeResourcesPath.resolve("cloudify-openstack-plugin/openstack-plugin.yaml"),
                generatedBlueprintDirectoryPath.resolve("openstack-plugin.yaml"));
        FileUtil.unzip(pluginRecipeResourcesPath.resolve("cloudify-openstack-plugin/cloudify-openstack-plugin.zip"),
                generatedBlueprintDirectoryPath.resolve("plugins"));
        Files.copy(pluginRecipeResourcesPath.resolve("cloudify-openstack-plugin/cloudify-openstack-plugin.zip"),
                generatedBlueprintDirectoryPath.resolve("plugins/cloudify-openstack-plugin.zip"));

        if (CollectionUtils.isNotEmpty(blueprintGeneratorExtensions)) {
            for (BlueprintGeneratorExtension blueprintGeneratorExtension : blueprintGeneratorExtensions) {
                blueprintGeneratorExtension.blueprintGenerationHook(pluginRecipeResourcesPath, generatedBlueprintDirectoryPath, context);
            }
        }

        // Copy kubernetes plugins
        FileUtil.unzip(pluginRecipeResourcesPath.resolve("cloudify-kubernetes-plugin/cloudify-kubernetes-plugin.zip"),
                generatedBlueprintDirectoryPath.resolve("plugins"));
        FileUtil.unzip(pluginRecipeResourcesPath.resolve("cloudify-proxy-plugin/cloudify-proxy-plugin.zip"),
                generatedBlueprintDirectoryPath.resolve("plugins"));

        // Docker types
        List<PaaSNodeTemplate> dockerTypes = new ArrayList<PaaSNodeTemplate>();
        Map<String, Map<String, Object>> docker_envs = new HashMap<String, Map<String, Object>>();
        for (PaaSNodeTemplate nonNative : alienDeployment.getNonNatives()) {
            if (ToscaUtils.isFromType(TOSCA_NODES_CONTAINER_APPLICATION_DOCKER_CONTAINER, nonNative.getIndexedToscaElement())) {
                dockerTypes.add(nonNative);

                // Retrieve env maps from the properties
                Map<String, Object> envMap = new LinkedHashMap<String, Object>();
                if (nonNative.getTemplate().getProperties().get("docker_env_vars") != null) {
                    envMap.putAll(((ComplexPropertyValue) nonNative.getTemplate().getProperties().get("docker_env_vars")).getValue());
                }

                // Retrieve the env maps from the interface (from create operation)
                Map<String, IValue> inputs = (Map<String, IValue>) nonNative.getInterfaces().get("tosca.interfaces.node.lifecycle.Standard").getOperations()
                        .get("create").getInputParameters();
                if (inputs != null) {
                    for (Map.Entry<String, IValue> entry : inputs.entrySet()) {
                        String key = entry.getKey();
                        String value = null;
                        if (entry.getValue() instanceof ScalarPropertyValue) {
                            value = ((ScalarPropertyValue) entry.getValue()).getValue();
                        } else {
                            value = entry.getValue().toString();
                        }
                        if (key.startsWith("ENV")) {
                            envMap.put(key.replaceFirst("^ENV_", ""), value);
                        }
                    }
                }

                Map<String, Object> podContext = MapUtil.newHashMap(new String[] { "dockerType", "envMap" }, new Object[] { nonNative, envMap });
                if (!envMap.isEmpty()) {
                    docker_envs.put(nonNative.getId(), envMap);
                }

                // Generate pod file
                Path podTemplatePath = pluginRecipeResourcesPath.resolve("kubernetes/pod.yaml.vm");
                Path podTargetPath = generatedBlueprintDirectoryPath.resolve(nonNative.getId() + "-pod.yaml");
                VelocityUtil.generate(podTemplatePath, podTargetPath, podContext);

                // Generate service file
                Path serviceTemplatePath = pluginRecipeResourcesPath.resolve("kubernetes/service.yaml.vm");
                Path serviceTargetPath = generatedBlueprintDirectoryPath.resolve(nonNative.getId() + "-service.yaml");
                VelocityUtil.generate(serviceTemplatePath, serviceTargetPath, podContext);
                // for(Capability capa : nonNative.getTemplate().getCapabilities().values()) {
                // ToscaUtils.isFromType(ALIEN_CAPABILITIES_ENDPOINT_DOCKER, capa.getType());
                // }
            }
        }
        // FIXME ugly remove docker types from non native types list
        alienDeployment.getNonNatives().removeAll(dockerTypes);
        if (!dockerTypes.isEmpty()) {
            context.put("docker_types", dockerTypes);

        }
        if (!docker_envs.isEmpty()) {
            context.put("docker_envs", docker_envs);
        }

        // Generate the blueprint at the end
            VelocityUtil.generate(pluginRecipeResourcesPath.resolve("velocity/blueprint.yaml.vm"), generatedBlueprintFilePath, context);
        return generatedBlueprintFilePath;
    }

    private OperationWrapper generateOperationScriptWrapper(String interfaceName, String operationName, Operation operation, IPaaSTemplate<?> owner,
            BlueprintGenerationUtil util, Map<String, Object> context, Path generatedBlueprintDirectoryPath,
            Map<String, Map<String, DeploymentArtifact>> artifacts, Map<Relationship, Map<String, DeploymentArtifact>> relationshipArtifacts,
            Map<String, PaaSNodeTemplate> allNodes) throws IOException {
        OperationWrapper operationWrapper = new OperationWrapper(owner, operation, interfaceName, operationName, artifacts, relationshipArtifacts,
                propertyEvaluatorService, allNodes);
        Map<String, Object> operationContext = Maps.newHashMap(context);
        operationContext.put("operation", operationWrapper);
        VelocityUtil.generate(pluginRecipeResourcesPath.resolve("velocity/script_wrapper.vm"),
                generatedBlueprintDirectoryPath
                        .resolve(util.getNonNative().getArtifactWrapperPath(owner, interfaceName, operationName, operation.getImplementationArtifact())),
                operationContext);
        return operationWrapper;
    }

    private void copyArtifact(Path generatedBlueprintDirectoryPath, Path csarPath, String pathToNode, IArtifact artifact, IArtifact originalArtifact)
            throws IOException {
        Path artifactPath;
        Path artifactCopiedPath;
        if (originalArtifact != null && ArtifactRepositoryConstants.ALIEN_ARTIFACT_REPOSITORY.equals(artifact.getArtifactRepository())) {
            // If the internal repository is used
            // Overridden artifact
            Path artifactCopiedDirectory = generatedBlueprintDirectoryPath
                    .resolve(mappingConfigurationHolder.getMappingConfiguration().getTopologyArtifactDirectoryName()).resolve(pathToNode)
                    .resolve(originalArtifact.getArchiveName());
            artifactPath = artifactRepository.resolveFile(artifact.getArtifactRef());
            artifactCopiedPath = artifactCopiedDirectory.resolve(originalArtifact.getArtifactRef());
        } else {
            Path artifactCopiedDirectory = generatedBlueprintDirectoryPath.resolve("artifacts").resolve(artifact.getArchiveName());
            FileSystem csarFS = FileSystems.newFileSystem(csarPath, null);
            String artifactRelativePathName = artifact.getArtifactRef();
            artifactPath = csarFS.getPath(artifactRelativePathName);
            artifactCopiedPath = artifactCopiedDirectory.resolve(artifactRelativePathName);
        }
        if (Files.isRegularFile(artifactCopiedPath)) {
            // already copied do nothing
            return;
        }
        Files.createDirectories(artifactCopiedPath.getParent());
        if (Files.isDirectory(artifactPath)) {
            FileUtil.copy(artifactPath, artifactCopiedPath, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.copy(artifactPath, artifactCopiedPath);
        }
    }

    private void copyDeploymentArtifacts(Path generatedBlueprintDirectoryPath, String pathToNode, AbstractTemplate node, IndexedArtifactToscaElement type)
            throws IOException, CSARVersionNotFoundException {
        Map<String, DeploymentArtifact> artifacts = type.getArtifacts();
        if (artifacts == null || artifacts.isEmpty()) {
            return;
        }
        for (Map.Entry<String, DeploymentArtifact> artifactEntry : artifacts.entrySet()) {
            DeploymentArtifact artifact = artifactEntry.getValue();
            if (artifact != null) {
                Path csarPath = repository.getCSAR(artifact.getArchiveName(), artifact.getArchiveVersion());
                Map<String, DeploymentArtifact> topologyArtifacts = node.getArtifacts();
                if (topologyArtifacts != null && topologyArtifacts.containsKey(artifactEntry.getKey())) {
                    copyArtifact(generatedBlueprintDirectoryPath, csarPath, pathToNode, topologyArtifacts.get(artifactEntry.getKey()), artifact);
                } else {
                    copyArtifact(generatedBlueprintDirectoryPath, csarPath, pathToNode, artifact, null);
                }
            }
        }
    }

    private void copyImplementationArtifacts(Path generatedBlueprintDirectoryPath, String pathToNode, Map<String, Interface> interfaces)
            throws IOException, CSARVersionNotFoundException {
        if (interfaces == null || interfaces.isEmpty()) {
            return;
        }
        // Copy implementation artifacts
        for (Map.Entry<String, Interface> interfaceEntry : interfaces.entrySet()) {
            Map<String, Operation> operations = interfaceEntry.getValue().getOperations();
            for (Map.Entry<String, Operation> operationEntry : operations.entrySet()) {
                ImplementationArtifact artifact = operationEntry.getValue().getImplementationArtifact();
                if (artifact != null && !artifact.getArtifactRef().endsWith(".dockerimg")) {
                    Path csarPath = repository.getCSAR(artifact.getArchiveName(), artifact.getArchiveVersion());
                    copyArtifact(generatedBlueprintDirectoryPath, csarPath, pathToNode, artifact, null);
                }
            }
        }
    }

    public Path resolveBlueprintPath(String deploymentId) {
        return recipeDirectoryPath.resolve(deploymentId);
    }

    @Required
    @Value("${directories.alien}/cloudify3")
    public void setCloudifyPath(final String path) throws IOException {
        Path cloudifyPath = Paths.get(path).toAbsolutePath();
        recipeDirectoryPath = cloudifyPath.resolve("recipes");
        Files.createDirectories(recipeDirectoryPath);
    }

    public static interface BlueprintGeneratorExtension {
        void blueprintGenerationHook(Path pluginRecipeResourcesPath, Path generatedBlueprintDirectoryPath, Map<String, Object> context) throws IOException;
    }

}
