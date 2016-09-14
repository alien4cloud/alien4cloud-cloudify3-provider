package alien4cloud.paas.cloudify3.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.component.repository.exception.CSARVersionNotFoundException;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.definitions.IArtifact;
import org.alien4cloud.tosca.model.definitions.ImplementationArtifact;
import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.definitions.Operation;
import alien4cloud.paas.IPaaSTemplate;
import alien4cloud.paas.cloudify3.blueprint.BlueprintGenerationUtil;
import alien4cloud.paas.cloudify3.blueprint.NonNativeTypeGenerationUtil;
import alien4cloud.paas.cloudify3.configuration.CloudConfigurationHolder;
import alien4cloud.paas.cloudify3.configuration.MappingConfigurationHolder;
import alien4cloud.paas.cloudify3.error.BlueprintGenerationException;
import alien4cloud.paas.cloudify3.service.model.CloudifyDeployment;
import alien4cloud.paas.cloudify3.service.model.OperationWrapper;
import alien4cloud.paas.cloudify3.service.model.Relationship;
import alien4cloud.paas.cloudify3.util.VelocityUtil;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;
import alien4cloud.plugin.model.ManagedPlugin;
import alien4cloud.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Handle blueprint generation from alien model
 *
 * @author Minh Khang VU
 */
@Component("cloudify-blueprint-service")
@Slf4j
public class BlueprintService {

    @Resource
    private CloudConfigurationHolder cloudConfigurationHolder;

    @Resource
    private MappingConfigurationHolder mappingConfigurationHolder;

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
        context.put("velocityResourcesPath", this.pluginRecipeResourcesPath.resolve("velocity"));

        // Copy artifacts
        for (PaaSNodeTemplate nonNative : alienDeployment.getNonNatives()) {
            // Don't process a node more than once
            copyDeploymentArtifacts(util.getNonNative(), generatedBlueprintDirectoryPath, nonNative);
            copyImplementationArtifacts(util.getNonNative(), generatedBlueprintDirectoryPath, nonNative);
            List<PaaSRelationshipTemplate> relationships = nonNative.getRelationshipTemplates();
            for (PaaSRelationshipTemplate relationship : relationships) {
                if (relationship.getSource().equals(nonNative.getId())) {
                    copyDeploymentArtifacts(util.getNonNative(), generatedBlueprintDirectoryPath, relationship);
                    copyImplementationArtifacts(util.getNonNative(), generatedBlueprintDirectoryPath, relationship);
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
                        if (MapUtils.isNotEmpty(node.getTemplate().getArtifacts())) {
                            artifacts.put(node.getId(), node.getTemplate().getArtifacts());
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
                                    relationship.getTemplate().getTarget());
                            Map<Relationship, Map<String, DeploymentArtifact>> relationshipArtifacts = Maps.newLinkedHashMap();
                            if (MapUtils.isNotEmpty(relationship.getTemplate().getArtifacts())) {
                                relationshipArtifacts.put(keyRelationship, relationship.getTemplate().getArtifacts());
                            }
                            Map<String, Map<String, DeploymentArtifact>> artifacts = Maps.newLinkedHashMap();
                            Map<String, DeploymentArtifact> sourceArtifacts = alienDeployment.getAllNodes().get(relationship.getSource()).getTemplate()
                                    .getArtifacts();
                            if (MapUtils.isNotEmpty(sourceArtifacts)) {
                                artifacts.put(relationship.getSource(), sourceArtifacts);
                            }
                            Map<String, DeploymentArtifact> targetArtifacts = alienDeployment.getAllNodes().get(relationship.getTemplate().getTarget())
                                    .getTemplate().getArtifacts();
                            if (MapUtils.isNotEmpty(targetArtifacts)) {
                                artifacts.put(relationship.getTemplate().getTarget(), targetArtifacts);
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

        if (CollectionUtils.isNotEmpty(blueprintGeneratorExtensions)) {
            for (BlueprintGeneratorExtension blueprintGeneratorExtension : blueprintGeneratorExtensions) {
                blueprintGeneratorExtension.blueprintGenerationHook(pluginRecipeResourcesPath, generatedBlueprintDirectoryPath, context);
            }
        }

        // Generate the blueprint at the end
        VelocityUtil.generate(pluginRecipeResourcesPath.resolve("velocity/blueprint.yaml.vm"), generatedBlueprintFilePath, context);
        return generatedBlueprintFilePath;
    }

    private OperationWrapper generateOperationScriptWrapper(String interfaceName, String operationName, Operation operation, IPaaSTemplate<?> owner,
            BlueprintGenerationUtil util, Map<String, Object> context, Path generatedBlueprintDirectoryPath,
            Map<String, Map<String, DeploymentArtifact>> artifacts, Map<Relationship, Map<String, DeploymentArtifact>> relationshipArtifacts,
            Map<String, PaaSNodeTemplate> allNodes) throws IOException {
        OperationWrapper operationWrapper = new OperationWrapper(owner, operation, interfaceName, operationName, artifacts, relationshipArtifacts);
        Map<String, Object> operationContext = Maps.newHashMap(context);
        operationContext.put("operation", operationWrapper);
        VelocityUtil.generate(pluginRecipeResourcesPath.resolve("velocity/script_wrapper.vm"),
                generatedBlueprintDirectoryPath.resolve(util.getNonNative().getArtifactWrapperPath(owner, interfaceName, operationName)), operationContext);
        return operationWrapper;
    }

    private void copyArtifact(Path artifactsDirectory, String pathToArtifact, IArtifact artifact) throws IOException {
        Path artifactCopiedPath = artifactsDirectory.resolve(pathToArtifact);
        Path artifactPath = Paths.get(artifact.getArtifactPath());
        ensureArtifactDefined(artifact, pathToArtifact);
        if (Files.isRegularFile(artifactCopiedPath)) {
            return;
        }
        Files.createDirectories(artifactCopiedPath.getParent());
        if (Files.isDirectory(artifactPath)) {
            FileUtil.copy(artifactPath, artifactCopiedPath, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.copy(artifactPath, artifactCopiedPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void ensureArtifactDefined(IArtifact artifact, String pathToNode) {
        if (artifact.getArtifactRef() == null || artifact.getArtifactRef().isEmpty()) {
            throw new BlueprintGenerationException(
                    "Cloudify plugin only manage deployment artifact with an artifact ref not null or empty. Failed to copy artifact of type <"
                            + artifact.getArtifactType() + "> for node <" + pathToNode + ">.");
        }
    }

    private void copyDeploymentArtifacts(NonNativeTypeGenerationUtil util, Path artifactsDir, IPaaSTemplate<?> node)
            throws IOException, CSARVersionNotFoundException {
        Map<String, DeploymentArtifact> artifacts = node.getTemplate().getArtifacts();
        if (MapUtils.isEmpty(artifacts)) {
            return;
        }
        for (Map.Entry<String, DeploymentArtifact> artifactEntry : artifacts.entrySet()) {
            DeploymentArtifact artifact = artifactEntry.getValue();
            if (artifact != null) {
                String relativePathToArtifact;
                if (node instanceof PaaSNodeTemplate) {
                    relativePathToArtifact = util.getArtifactPath(node.getId(), artifactEntry.getKey(), artifact);
                } else if (node instanceof PaaSRelationshipTemplate) {
                    PaaSRelationshipTemplate relationshipTemplate = ((PaaSRelationshipTemplate) node);
                    relativePathToArtifact = util.getRelationshipArtifactPath(relationshipTemplate.getSource(), relationshipTemplate.getId(),
                            artifactEntry.getKey(), artifact);
                } else {
                    throw new UnsupportedOperationException("Unsupported artifact copy for " + node.getClass().getName());
                }
                copyArtifact(artifactsDir, relativePathToArtifact, artifact);
            }
        }
    }

    private void copyImplementationArtifacts(NonNativeTypeGenerationUtil util, Path artifactsDir, IPaaSTemplate<?> node)
            throws IOException, CSARVersionNotFoundException {
        Map<String, Interface> interfaces = node.getInterfaces();
        if (interfaces == null || interfaces.isEmpty()) {
            return;
        }
        // Copy implementation artifacts
        for (Map.Entry<String, Interface> interfaceEntry : interfaces.entrySet()) {
            Map<String, Operation> operations = interfaceEntry.getValue().getOperations();
            for (Map.Entry<String, Operation> operationEntry : operations.entrySet()) {
                ImplementationArtifact artifact = operationEntry.getValue().getImplementationArtifact();
                if (artifact != null) {
                    String relativePathToArtifact;
                    if (node instanceof PaaSNodeTemplate) {
                        relativePathToArtifact = util.getImplementationArtifactPath((PaaSNodeTemplate) node, interfaceEntry.getKey(), operationEntry.getKey(),
                                artifact);
                    } else if (node instanceof PaaSRelationshipTemplate) {
                        PaaSRelationshipTemplate relationshipTemplate = ((PaaSRelationshipTemplate) node);
                        relativePathToArtifact = util.getRelationshipImplementationArtifactPath(relationshipTemplate, interfaceEntry.getKey(),
                                operationEntry.getKey(), artifact);
                    } else {
                        throw new UnsupportedOperationException("Unsupported artifact copy for " + node.getClass().getName());
                    }
                    copyArtifact(artifactsDir, relativePathToArtifact, artifact);
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

    public interface BlueprintGeneratorExtension {
        void blueprintGenerationHook(Path pluginRecipeResourcesPath, Path generatedBlueprintDirectoryPath, Map<String, Object> context) throws IOException;
    }

}
