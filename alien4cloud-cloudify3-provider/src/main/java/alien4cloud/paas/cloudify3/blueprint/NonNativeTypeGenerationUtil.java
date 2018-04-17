package alien4cloud.paas.cloudify3.blueprint;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.paas.IPaaSTemplate;
import alien4cloud.paas.cloudify3.artifacts.ICloudifyImplementationArtifact;
import alien4cloud.paas.cloudify3.configuration.MappingConfiguration;
import alien4cloud.paas.cloudify3.service.PropertyEvaluatorService;
import alien4cloud.paas.cloudify3.service.model.CloudifyDeployment;
import alien4cloud.paas.cloudify3.service.model.OperationWrapper;
import alien4cloud.paas.cloudify3.service.model.Relationship;
import alien4cloud.paas.cloudify3.shared.ArtifactRegistryService;
import alien4cloud.paas.exception.NotSupportedException;
import alien4cloud.paas.function.FunctionEvaluator;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;
import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.tosca.PaaSUtils;
import alien4cloud.utils.FileUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;
import org.alien4cloud.tosca.model.definitions.ConcatPropertyValue;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.IValue;
import org.alien4cloud.tosca.model.definitions.ImplementationArtifact;
import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.definitions.ListPropertyValue;
import org.alien4cloud.tosca.model.definitions.Operation;
import org.alien4cloud.tosca.model.definitions.OperationOutput;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.templates.ServiceNodeTemplate;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.normative.constants.NormativeCapabilityTypes;
import org.alien4cloud.tosca.normative.constants.NormativeComputeConstants;
import org.alien4cloud.tosca.normative.constants.NormativeRelationshipConstants;
import org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants;
import org.alien4cloud.tosca.utils.TopologyUtils;
import org.alien4cloud.tosca.utils.ToscaTypeUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class NonNativeTypeGenerationUtil extends AbstractGenerationUtil {
    private ArtifactRegistryService artifactRegistryService;

    public NonNativeTypeGenerationUtil(MappingConfiguration mappingConfiguration, CloudifyDeployment alienDeployment, Path recipePath,
            PropertyEvaluatorService propertyEvaluatorService, ArtifactRegistryService artifactRegistryService) {
        super(mappingConfiguration, alienDeployment, recipePath, propertyEvaluatorService);
        this.artifactRegistryService = artifactRegistryService;
    }

    public boolean isStandardLifecycleInterface(String interfaceName) {
        return ToscaNodeLifecycleConstants.STANDARD.equals(interfaceName) || ToscaNodeLifecycleConstants.STANDARD_SHORT.equals(interfaceName);
    }

    public boolean propertyCanBeExposed(AbstractPropertyValue propertyValue) {
        return propertyValue != null && !org.alien4cloud.tosca.utils.FunctionEvaluator.containGetSecretFunction(propertyValue);
    }

    public boolean attributeCanBeExposed(AbstractPropertyValue attributeValue) {
        return attributeValue != null && !org.alien4cloud.tosca.utils.FunctionEvaluator.containGetSecretFunction(attributeValue);
    }

    public String tryToMapToCloudifyInterface(String interfaceName) {
        if (isStandardLifecycleInterface(interfaceName)) {
            return "cloudify.interfaces.lifecycle";
        } else {
            return interfaceName;
        }
    }

    public String tryToMapToCloudifyRelationshipInterface(String interfaceName) {
        if (ToscaRelationshipLifecycleConstants.CONFIGURE.equals(interfaceName) || ToscaRelationshipLifecycleConstants.CONFIGURE_SHORT.equals(interfaceName)) {
            return "cloudify.interfaces.relationship_lifecycle";
        } else {
            return interfaceName;
        }
    }

    public String tryToMapToCloudifyRelationshipOperation(String interfaceName, String operationName) {
        if (ToscaRelationshipLifecycleConstants.CONFIGURE.equals(interfaceName) || ToscaRelationshipLifecycleConstants.CONFIGURE_SHORT.equals(interfaceName)) {
            switch (operationName) {
            case ToscaRelationshipLifecycleConstants.PRE_CONFIGURE_SOURCE:
            case ToscaRelationshipLifecycleConstants.PRE_CONFIGURE_TARGET:
                return "preconfigure";
            case ToscaRelationshipLifecycleConstants.POST_CONFIGURE_SOURCE:
            case ToscaRelationshipLifecycleConstants.POST_CONFIGURE_TARGET:
                return "postconfigure";
            case ToscaRelationshipLifecycleConstants.ADD_SOURCE:
            case ToscaRelationshipLifecycleConstants.ADD_TARGET:
                return "establish";
            case ToscaRelationshipLifecycleConstants.REMOVE_SOURCE:
            case ToscaRelationshipLifecycleConstants.REMOVE_TARGET:
                return "unlink";
            default:
                return operationName;
            }
        } else {
            return operationName;
        }
    }

    public Map<String, Interface> filterRelationshipSourceInterfaces(Map<String, Interface> interfaces) {
        return TopologyUtils.filterInterfaces(interfaces, mappingConfiguration.getRelationships().getLifeCycle().getSource().keySet());
    }

    public Map<String, Interface> filterRelationshipTargetInterfaces(Map<String, Interface> interfaces) {
        return TopologyUtils.filterInterfaces(interfaces, mappingConfiguration.getRelationships().getLifeCycle().getTarget().keySet());
    }

    public Map<String, Interface> getNodeInterfaces(PaaSNodeTemplate node) {
        return TopologyUtils.filterAbstractInterfaces(node.getInterfaces());
    }

    public Map<String, Interface> getRelationshipInterfaces(PaaSRelationshipTemplate relationship) {
        return TopologyUtils.filterAbstractInterfaces(relationship.getInterfaces());
    }

    public Map<String, IValue> getNodeAttributes(PaaSNodeTemplate nodeTemplate) {
        if (!isNonNative(nodeTemplate)) {
            // Do not try to publish attributes for non native nodes
            return null;
        }
        if (MapUtils.isEmpty(nodeTemplate.getNodeTemplate().getAttributes())) {
            return null;
        }
        Map<String, IValue> attributesThatCanBeSet = Maps.newLinkedHashMap();
        for (Map.Entry<String, IValue> attributeEntry : nodeTemplate.getNodeTemplate().getAttributes().entrySet()) {
            if (attributeEntry.getValue() instanceof AbstractPropertyValue) {
                // Replace all get_property with the static value in all attributes
                attributesThatCanBeSet.put(attributeEntry.getKey(), attributeEntry.getValue());
            }
        }
        return attributesThatCanBeSet;
    }

    // TODO: manage concat ?
    public Map<String, String> getNodeProperties(PaaSNodeTemplate node) {
        Map<String, String> propertyValues = Maps.newHashMap();
        Map<String, AbstractPropertyValue> nodeProperties = node.getTemplate().getProperties();
        if (MapUtils.isNotEmpty(nodeProperties)) {
            for (Entry<String, AbstractPropertyValue> propertyEntry : nodeProperties.entrySet()) {
                String propertyName = propertyEntry.getKey();
                String propertyValue = null;
                if (propertyEntry.getValue() instanceof FunctionPropertyValue) {
                    FunctionPropertyValue function = (FunctionPropertyValue) propertyEntry.getValue();
                    if (ToscaFunctionConstants.GET_PROPERTY.equals(function.getFunction())) {
                        propertyValue = FunctionEvaluator.evaluateGetPropertyFunction(function, node, alienDeployment.getAllNodes());
                    }
                } else if (propertyEntry.getValue() instanceof ScalarPropertyValue) {
                    propertyValue = ((ScalarPropertyValue) propertyEntry.getValue()).getValue();
                }
                if (propertyValue != null) {
                    propertyValues.put(propertyName, propertyValue);
                }
            }
        }
        return propertyValues;
    }

    public PaaSNodeTemplate getSourceNode(PaaSRelationshipTemplate relationshipTemplate) {
        return alienDeployment.getAllNodes().get(relationshipTemplate.getSource());
    }

    public PaaSNodeTemplate getTargetNode(PaaSRelationshipTemplate relationshipTemplate) {
        return alienDeployment.getAllNodes().get(relationshipTemplate.getRelationshipTemplate().getTarget());
    }

    public Map<String, IValue> getSourceRelationshipAttributes(PaaSRelationshipTemplate owner) {
        return getNodeAttributes(getSourceNode(owner));
    }

    public Map<String, IValue> getTargetRelationshipAttributes(PaaSRelationshipTemplate owner) {
        return getNodeAttributes(getTargetNode(owner));
    }

    public boolean isGetAttributeFunctionPropertyValue(IValue input) {
        return (input instanceof FunctionPropertyValue) && ToscaFunctionConstants.GET_ATTRIBUTE.equals(((FunctionPropertyValue) input).getFunction());
    }

    public boolean isFunctionPropertyValue(IValue input) {
        return input instanceof FunctionPropertyValue;
    }

    public boolean isConcatPropertyValue(IValue input) {
        return input instanceof ConcatPropertyValue;
    }

    public String formatValue(IPaaSTemplate<?> owner, IValue input) throws JsonProcessingException {
        if (input instanceof FunctionPropertyValue) {
            return formatFunctionPropertyValue(owner, (FunctionPropertyValue) input);
        } else if (input instanceof ConcatPropertyValue) {
            return formatConcatPropertyValue(owner, (ConcatPropertyValue) input);
        } else if (input instanceof ScalarPropertyValue) {
            return formatTextValueToPython(((ScalarPropertyValue) input).getValue());
        } else if (input instanceof ComplexPropertyValue || input instanceof ListPropertyValue) {
            return formatTextValueToPython(JsonUtil.toString(((PropertyValue) input).getValue()));
        } else if (input instanceof PropertyDefinition) {
            // Custom command do nothing
            return "''";
        } else {
            String ownerId = owner == null ? "" : "for " + owner.getId();
            throw new NotSupportedException("The value " + input + "'s type is not supported as operation input for owner " + ownerId);
        }
    }

    public String formatTextValueToPython(String text) {
        if (StringUtils.isEmpty(text)) {
            return "''";
        }
        // Even in raw string the backslash need to be escaped so env variable export works correctly.
        String escaped = text.replaceAll(Pattern.quote("\\"), Matcher.quoteReplacement("\\\\"));
        // In the case we have 3 simple quotes we don't want the string to end so concatenate a python variable we add to the velocity files.
        escaped = escaped.replaceAll(Pattern.quote("'''"), Matcher.quoteReplacement("'''+sqotes+'''"));
        return "r'''" + escaped + "'''";
    }

    public String formatConcatPropertyValue(IPaaSTemplate<?> owner, ConcatPropertyValue concatPropertyValue) {
        return formatConcatPropertyValue("", owner, concatPropertyValue);
    }

    public String formatConcatPropertyValue(String context, IPaaSTemplate<?> owner, ConcatPropertyValue concatPropertyValue) {
        StringBuilder pythonCall = new StringBuilder();
        if (concatPropertyValue.getParameters() == null || concatPropertyValue.getParameters().isEmpty()) {
            throw new InvalidArgumentException("Parameter list for concat function is empty");
        }
        for (IValue concatParam : concatPropertyValue.getParameters()) {
            // scalar type
            if (concatParam instanceof ScalarPropertyValue) {
                // scalar case
                String value = ((ScalarPropertyValue) concatParam).getValue();
                if (StringUtils.isNotEmpty(value)) {
                    pythonCall.append(formatTextValueToPython(value)).append(" + ");
                }
            } else if (concatParam instanceof PropertyDefinition) {
                throw new NotSupportedException("Do not support property definition in a concat");
            } else if (concatParam instanceof FunctionPropertyValue) {
                // Function case
                FunctionPropertyValue functionPropertyValue = (FunctionPropertyValue) concatParam;
                pythonCall.append(formatFunctionPropertyValue(context, owner, functionPropertyValue)).append(" + ");
            } else {
                throw new NotSupportedException("Do not support nested concat in a concat, please simplify your usage");
            }
        }
        // Remove the last " + "
        pythonCall.setLength(pythonCall.length() - 3);
        return pythonCall.toString();
    }

    public String formatFunctionPropertyValue(IPaaSTemplate<?> owner, FunctionPropertyValue functionPropertyValue) {
        return formatFunctionPropertyValue("", owner, functionPropertyValue);
    }

    public String formatFunctionPropertyValue(String context, IPaaSTemplate<?> owner, FunctionPropertyValue functionPropertyValue) {
        if (owner instanceof PaaSNodeTemplate) {
            return formatNodeFunctionPropertyValue(context, functionPropertyValue);
        } else if (owner instanceof PaaSRelationshipTemplate) {
            return formatRelationshipFunctionPropertyValue(context, (PaaSRelationshipTemplate) owner, functionPropertyValue);
        } else {
            throw new NotSupportedException("Un-managed paaS template type " + owner.getClass().getSimpleName());
        }
    }

    /**
     * Format operation parameter of a node
     *
     * @param functionPropertyValue the input which can be a function or a scalar
     * @return the formatted parameter understandable by Cloudify
     */
    public String formatNodeFunctionPropertyValue(String context, FunctionPropertyValue functionPropertyValue) {
        switch (functionPropertyValue.getFunction()) {
        case ToscaFunctionConstants.GET_SECRET:
            return "get_secret('" + functionPropertyValue.getTemplateName() + "')";
        case ToscaFunctionConstants.GET_ATTRIBUTE:
            return "get_attribute(ctx" + context + ", '" + functionPropertyValue.getElementNameToFetch() + "')";
        case ToscaFunctionConstants.GET_PROPERTY:
            return "get_property(ctx" + context + ", '" + functionPropertyValue.getElementNameToFetch() + "')";
        case ToscaFunctionConstants.GET_OPERATION_OUTPUT:
            // a fake attribute is used in order to handle Operation Outputs
            return "get_attribute(ctx" + context + ", '_a4c_OO:" + functionPropertyValue.getInterfaceName() + ':' + functionPropertyValue.getOperationName()
                    + ":" + functionPropertyValue.getElementNameToFetch() + "')";
        default:
            throw new NotSupportedException("Function " + functionPropertyValue.getFunction() + " is not yet supported");
        }
    }

    /**
     * Format operation parameter of a node
     *
     * @param functionPropertyValue the input which can be a function or a scalar
     * @param relationshipTemplate  The relationship template for which to format the function request.
     * @return the formatted parameter understandable by Cloudify
     */
    private String formatRelationshipFunctionPropertyValue(String context, PaaSRelationshipTemplate relationshipTemplate,
            FunctionPropertyValue functionPropertyValue) {
        switch (functionPropertyValue.getFunction()) {
        case ToscaFunctionConstants.GET_SECRET:
            return "get_secret('" + functionPropertyValue.getTemplateName() + "')";
        case ToscaFunctionConstants.GET_ATTRIBUTE:
            if (ToscaFunctionConstants.R_TARGET.equals(functionPropertyValue.getTemplateName().toUpperCase())
                    && relationshipTemplate.getTemplate().getTargetedCapabilityName() != null) {
                // If fetching from target and we know then try to fetch attribute from the target capability first and then the from the node.
                return "get_target_capa_or_node_attribute(ctx." + ToscaFunctionConstants.TARGET.toLowerCase() + context + ", 'capabilities."
                        + relationshipTemplate.getTemplate().getTargetedCapabilityName() + "." + functionPropertyValue.getElementNameToFetch() + "', '"
                        + functionPropertyValue.getElementNameToFetch() + "')";
            } else if (ToscaFunctionConstants.SOURCE.equals(functionPropertyValue.getTemplateName().toUpperCase())) {
                // If fetching from source and we know then try to fetch attribute from the target requirement first and then the from the node.
                return "get_target_capa_or_node_attribute(ctx." + functionPropertyValue.getTemplateName().toLowerCase() + context + ", 'requirements."
                        + relationshipTemplate.getTemplate().getRequirementName() + "." + functionPropertyValue.getElementNameToFetch() + "', '"
                        + functionPropertyValue.getElementNameToFetch() + "')";
            }
            if (functionPropertyValue.getParameters().size() > 2) {
                StringBuilder builder = new StringBuilder();
                builder.append("get_nested_attribute(ctx.").append(functionPropertyValue.getTemplateName().toLowerCase()).append(context).append(", [");
                for (int i = 1; i < functionPropertyValue.getParameters().size(); i++) {
                    if (i > 1) {
                        builder.append(", ");
                    }
                    builder.append("'").append(functionPropertyValue.getParameters().get(i)).append("'");
                }
                builder.append("])");
                return builder.toString();
            }
            return "get_attribute(ctx." + functionPropertyValue.getTemplateName().toLowerCase() + context + ", '" + functionPropertyValue
                    .getElementNameToFetch() + "')";
        case ToscaFunctionConstants.GET_PROPERTY:
            return "get_property(ctx." + functionPropertyValue.getTemplateName().toLowerCase() + context + ", '" + functionPropertyValue.getElementNameToFetch()
                    + "')";
        case ToscaFunctionConstants.GET_OPERATION_OUTPUT:
            return "get_attribute(ctx." + functionPropertyValue.getTemplateName().toLowerCase() + context + ", '_a4c_OO:" + functionPropertyValue
                    .getInterfaceName() + ':' + functionPropertyValue.getOperationName() + ":" + functionPropertyValue.getElementNameToFetch() + "')";
        default:
            throw new NotSupportedException("Function " + functionPropertyValue.getFunction() + " is not supported");
        }
    }

    public List<PaaSRelationshipTemplate> getSourceRelationships(PaaSNodeTemplate nodeTemplate) {
        List<PaaSRelationshipTemplate> relationshipTemplates = nodeTemplate.getRelationshipTemplates();
        List<PaaSRelationshipTemplate> sourceRelationshipTemplates = Lists.newArrayList();
        for (PaaSRelationshipTemplate relationshipTemplate : relationshipTemplates) {
            if (relationshipTemplate.getSource().equals(nodeTemplate.getId())) {
                sourceRelationshipTemplates.add(relationshipTemplate);
            }
        }
        return sourceRelationshipTemplates;
    }

    private String tryToMapToCloudifyRelationshipInterfaceOperation(String operationName, boolean isSource) {
        Map<String, String> mapping;
        if (isSource) {
            mapping = mappingConfiguration.getRelationships().getLifeCycle().getSource();
        } else {
            mapping = mappingConfiguration.getRelationships().getLifeCycle().getTarget();
        }
        String mappedName = mapping.get(operationName);
        return mappedName != null ? mappedName : operationName;
    }

    public String tryToMapToCloudifyRelationshipSourceInterfaceOperation(String operationName) {
        return tryToMapToCloudifyRelationshipInterfaceOperation(operationName, true);
    }

    public String tryToMapToCloudifyRelationshipTargetInterfaceOperation(String operationName) {
        return tryToMapToCloudifyRelationshipInterfaceOperation(operationName, false);
    }

    public String getDerivedFromType(List<String> allDerivedFromsTypes) {
        for (String derivedFromType : allDerivedFromsTypes) {
            if (typeMustBeMappedToCloudifyType(derivedFromType)) {
                return tryToMapToCloudifyType(derivedFromType);
            }
        }
        // This must never happens
        return allDerivedFromsTypes.get(0);
    }

    public PaaSNodeTemplate getHost(PaaSNodeTemplate node) {
        return PaaSUtils.getMandatoryHostTemplate(node);
    }

    public boolean relationshipHasDeploymentArtifacts(PaaSRelationshipTemplate relationshipTemplate) {
        return alienDeployment.getAllRelationshipDeploymentArtifacts().containsKey(
                new Relationship(relationshipTemplate.getId(), relationshipTemplate.getSource(), relationshipTemplate.getRelationshipTemplate().getTarget()));
    }

    public List<Relationship> getAllRelationshipWithDeploymentArtifacts(PaaSNodeTemplate nodeTemplate) {
        List<Relationship> relationships = Lists.newArrayList();
        for (Relationship relationship : alienDeployment.getAllRelationshipDeploymentArtifacts().keySet()) {
            if (relationship.getTarget().equals(nodeTemplate.getId())) {
                relationships.add(relationship);
            }
        }
        return relationships;
    }

    public Map<String, String> listArtifactDirectory(final String artifactPath) throws IOException {
        final Map<String, String> children = Maps.newHashMap();
        final Path realArtifactPath = recipePath.resolve(artifactPath);
        Files.walkFileTree(realArtifactPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String relativePath = FileUtil.getChildEntryRelativePath(realArtifactPath, file, true);
                String absolutePath = FileUtil.getChildEntryRelativePath(recipePath, file, true);
                children.put(relativePath, absolutePath);
                return super.visitFile(file, attrs);
            }
        });
        return children;
    }

    public boolean isArtifactDirectory(String artifactPath) {
        return Files.isDirectory(recipePath.resolve(artifactPath));
    }

    public boolean operationHasDeploymentArtifacts(OperationWrapper operationWrapper) {
        return MapUtils.isNotEmpty(operationWrapper.getAllDeploymentArtifacts()) || MapUtils
                .isNotEmpty(operationWrapper.getAllRelationshipDeploymentArtifacts());
    }

    public String getOperationOutputNames(Operation operation) {
        if (operation.getOutputs() != null && !operation.getOutputs().isEmpty()) {
            StringBuilder result = new StringBuilder();
            for (OperationOutput operationOutput : operation.getOutputs()) {
                if (result.length() > 0) {
                    result.append(";");
                }
                result.append(operationOutput.getName());
            }
            return result.toString();
        } else {
            return null;
        }
    }

    public boolean shouldRaiseExceptionOnFailure(OperationWrapper operationWrapper) {
        if (ToscaNodeLifecycleConstants.STANDARD.equals(operationWrapper.getInterfaceName())) {
            switch (operationWrapper.getOperationName()) {
            case ToscaNodeLifecycleConstants.STOP:
            case ToscaNodeLifecycleConstants.DELETE:
                return false;
            }
        }

        if (ToscaRelationshipLifecycleConstants.CONFIGURE.equals(operationWrapper.getInterfaceName())) {
            switch (operationWrapper.getOperationName()) {
            case ToscaRelationshipLifecycleConstants.REMOVE_TARGET:
            case ToscaRelationshipLifecycleConstants.REMOVE_SOURCE:
                return false;
            }
        }

        return true;
    }

    public boolean isOperationOwnedByRelationship(OperationWrapper operationWrapper) {
        return (operationWrapper.getOwner() instanceof PaaSRelationshipTemplate);
    }

    public boolean isOperationOwnedByNode(OperationWrapper operationWrapper) {
        return (operationWrapper.getOwner() instanceof PaaSNodeTemplate);
    }

    public boolean isNonNative(PaaSNodeTemplate nodeTemplate) {
        return alienDeployment.getNonNatives().contains(nodeTemplate);
    }

    public String getArtifactPath(String nodeId, String artifactId, DeploymentArtifact artifact) {
        return mappingConfiguration.getArtifactDirectoryName() + "/" + nodeId + "/" + artifactId + "/" + Paths.get(artifact.getArtifactPath()).getFileName()
                .toString();
    }

    public String getRelationshipArtifactPath(String sourceId, String relationshipId, String artifactId, DeploymentArtifact artifact) {
        return mappingConfiguration.getArtifactDirectoryName() + "/" + sourceId + "/" + relationshipId + "/" + artifactId + "/" + Paths
                .get(artifact.getArtifactPath()).getFileName().toString();
    }

    public String getImplementationArtifactPath(PaaSNodeTemplate owner, String interfaceName, String operationName, ImplementationArtifact artifact) {
        return mappingConfiguration.getImplementationArtifactDirectoryName() + "/" + owner.getId() + "/" + interfaceName + "/" + operationName + "/" + Paths
                .get(artifact.getArtifactPath()).getFileName().toString();
    }

    public String getRelationshipImplementationArtifactPath(PaaSRelationshipTemplate owner, String interfaceName, String operationName,
            ImplementationArtifact artifact) {
        return mappingConfiguration.getImplementationArtifactDirectoryName() + "/" + owner.getSource() + "_" + owner.getTemplate().getTarget() + "/" + owner
                .getId() + "/" + interfaceName + "/" + operationName + "/" + Paths.get(artifact.getArtifactPath()).getFileName().toString();
    }

    public String getArtifactWrapperPath(IPaaSTemplate<?> owner, String interfaceName, String operationName) {
        String wrapperPath = mappingConfiguration.getGeneratedArtifactPrefix() + "_" + operationName + ".py";
        if (owner instanceof PaaSNodeTemplate) {
            PaaSNodeTemplate ownerNode = (PaaSNodeTemplate) owner;
            return "wrapper/" + ownerNode.getId() + "/" + interfaceName + "/" + operationName + "/" + wrapperPath;
        } else if (owner instanceof PaaSRelationshipTemplate) {
            PaaSRelationshipTemplate ownerRelationship = (PaaSRelationshipTemplate) owner;
            return "wrapper/" + ownerRelationship.getSource() + "_" + ownerRelationship.getTemplate().getTarget() + "/" + ownerRelationship.getId() + "/"
                    + interfaceName + "/" + operationName + "/" + wrapperPath;
        } else {
            throw new NotSupportedException("Not supported template type " + owner.getId());
        }
    }

    /**
     * Utility method to know where the operation should be executed (on the host node or management node).
     *
     * @param operation The operation for which to check hosting.
     * @return True if the operation should be executed on the host node and false if the operation should be executed on the management agent.
     */
    public boolean isHostAgent(PaaSNodeTemplate node, Operation operation) {

        if (isCustomResource(node)) {
            return false;
        }

        if (isNonNative(node)) {
            PaaSNodeTemplate topParentNode = getTopParentPaaSNodeTemplate(node);
            if (!isCompute(topParentNode) && !isServiceNodeTemplate(topParentNode) && isCustomResource(topParentNode)) {
                // If the top parent node is a custom resource but not a compute nor a service,
                // operations of the hosted node are executed on the management agent's side.
                return false;
            }
        }

        // If the node is compute only the create operation is executed on central node. Other operations are called on the compute instance (that we should be
        // able to connect to after create).

        // We also check the artifact as some artifacts are anyway executed on central node.
        ICloudifyImplementationArtifact cloudifyImplementationArtifact = artifactRegistryService
                .getCloudifyImplementationArtifact(operation.getImplementationArtifact().getArtifactType());
        if (cloudifyImplementationArtifact != null) {
            return cloudifyImplementationArtifact.hostAgentExecution();
        }

        return true;
    }

    /**
     * Iterates over the hostedOn relationship and returns the top parent node.
     *
     * @param node The node to get the parent
     * @return The top parent node of the given node
     */
    private PaaSNodeTemplate getTopParentPaaSNodeTemplate(PaaSNodeTemplate node) {
        for (PaaSRelationshipTemplate relationshipTemplate : node.getRelationshipTemplates()) {
            if (relationshipTemplate.instanceOf(NormativeRelationshipConstants.HOSTED_ON) && !node.getId()
                    .equals(this.getTargetNode(relationshipTemplate).getId())) {
                return this.getTopParentPaaSNodeTemplate(this.getTargetNode(relationshipTemplate));
            }
        }
        return node;
    }

    /**
     * In the node properties, isolate:
     * <ul>
     * <li>those related to cloudify type inherited properties.</li>
     * <li>(deprecated) properties that can be serialized as string (for kubernetes)</li>
     * </ul>
     */
    public Map<String, AbstractPropertyValue> getCloudifyAndSimpleProperties(PaaSNodeTemplate node) {
        String cloudifyType = this.getDerivedFromType(node.getIndexedToscaElement().getDerivedFrom());
        Set<String> cloudifyProperies = this.mappingConfiguration.getCloudifyProperties().get(cloudifyType);
        Map<String, String> propertyValuesAsString = this.getNodeProperties(node);
        Map<String, AbstractPropertyValue> result = Maps.newHashMap();
        if (cloudifyProperies == null) {
            cloudifyProperies = Sets.newHashSet();
        }
        for (Entry<String, AbstractPropertyValue> e : node.getTemplate().getProperties().entrySet()) {
            if (cloudifyProperies.contains(e.getKey())) {
                // for custom native nodes we add inherited cloudify properties
                result.put(e.getKey(), e.getValue());
            } else if (propertyValuesAsString.containsKey(e.getKey())) {
                result.put(e.getKey(), new ScalarPropertyValue(propertyValuesAsString.get(e.getKey())));
            }
        }
        return result;
    }

    /**
     * In the node propertiy definitions, exclude those inherited from cloudify type.
     */
    public Map<String, PropertyDefinition> excludeCloudifyPropertyDefinitions(NodeType nodeType) {
        String cloudifyType = this.getDerivedFromType(nodeType.getDerivedFrom());
        Set<String> cloudifyProperies = this.mappingConfiguration.getCloudifyProperties().get(cloudifyType);

        Map<String, PropertyDefinition> result = Maps.newHashMap();
        if (cloudifyProperies != null && !cloudifyProperies.isEmpty()) {
            for (Entry<String, PropertyDefinition> e : nodeType.getProperties().entrySet()) {
                if (!cloudifyProperies.contains(e.getKey())) {
                    result.put(e.getKey(), e.getValue());
                }
            }
        } else if (nodeType.getProperties() != null) {
            result.putAll(nodeType.getProperties());
        }
        return result;
    }

    /**
     * A custom resource is a template that:
     * <ul>
     * <li>is not of a type provided by the location</li>
     * <li>AND doesn't have a host</li>
     * </ul>
     *
     * @param node
     * @return true is the node is considered as a custom template.
     */
    public boolean isCustomResource(PaaSNodeTemplate node) {
        return this.alienDeployment.getCustomResources().containsValue(node);
    }

    public boolean isCompute(PaaSNodeTemplate node) {
        return ToscaTypeUtils.isOfType(node.getIndexedToscaElement(), NormativeComputeConstants.COMPUTE_TYPE);
    }

    public boolean isServiceNodeTemplate(PaaSNodeTemplate node) {
        return node.getTemplate() instanceof ServiceNodeTemplate;
    }

    public boolean isEndpoint(String capabilityTypeName) {
        CapabilityType capabilityType = alienDeployment.getCapabilityTypes().get(capabilityTypeName);
        return capabilityType != null && (NormativeCapabilityTypes.ENDPOINT.equals(capabilityType.getElementId()) || (capabilityType.getDerivedFrom() != null
                && capabilityType.getDerivedFrom().contains(NormativeCapabilityTypes.ENDPOINT)));
    }
}
