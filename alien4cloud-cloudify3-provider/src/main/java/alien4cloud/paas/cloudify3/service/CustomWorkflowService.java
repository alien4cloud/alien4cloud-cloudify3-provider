package alien4cloud.paas.cloudify3.service;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.model.definitions.IValue;
import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.definitions.Operation;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import alien4cloud.paas.cloudify3.blueprint.BlueprintGenerationUtil;
import alien4cloud.paas.cloudify3.configuration.CfyConnectionManager;
import alien4cloud.paas.cloudify3.configuration.MappingConfigurationHolder;
import alien4cloud.paas.cloudify3.model.Deployment;
import alien4cloud.paas.cloudify3.model.NodeInstance;
import alien4cloud.paas.cloudify3.model.Workflow;
import alien4cloud.paas.cloudify3.service.model.CloudifyDeployment;
import alien4cloud.paas.cloudify3.shared.ArtifactRegistryService;
import alien4cloud.paas.exception.OperationExecutionException;
import alien4cloud.paas.model.NodeOperationExecRequest;
import alien4cloud.paas.model.PaaSNodeTemplate;

/**
 * Handle custom workflow (non lifecycle workflow) which permit to modify the deployment at runtime
 *
 * @author Minh Khang VU
 */
@Component("cloudify-custom-workflow-service")
public class CustomWorkflowService extends RuntimeService {
    @Resource
    private MappingConfigurationHolder mappingConfigurationHolder;
    @Resource
    private BlueprintService blueprintService;
    @Resource
    private PropertyEvaluatorService propertyEvaluatorService;
    @Resource
    private OrchestratorDeploymentPropertiesService deploymentPropertiesService;
    @Inject
    private ArtifactRegistryService artifactRegistryService;
    @Inject
    private CfyConnectionManager configurationHolder;

    private Map<String, Object> buildWorkflowParameters(NodeOperationExecRequest nodeOperationExecRequest, Operation operation) {
        Map<String, Object> workflowParameters = Maps.newHashMap();
        workflowParameters.put("operation", nodeOperationExecRequest.getInterfaceName() + "." + nodeOperationExecRequest.getOperationName());
        if (StringUtils.isNotBlank(nodeOperationExecRequest.getInstanceId())) {
            workflowParameters.put("node_instance_ids", new String[] { nodeOperationExecRequest.getInstanceId() });
        }
        if (StringUtils.isNotBlank(nodeOperationExecRequest.getNodeTemplateName())) {
            workflowParameters.put("node_ids", new String[] { nodeOperationExecRequest.getNodeTemplateName() });
        }
        Map<String, IValue> inputParameters = null;
        if (operation != null) {
            // operation can be null in case of operation only known at blueprint level
            inputParameters = operation.getInputParameters();
        }
        // operation_kwargs --> process --> env
        Map<String, Object> inputs = Maps.newHashMap();
        workflowParameters.put("operation_kwargs", inputs);

        if (MapUtils.isNotEmpty(inputParameters) || MapUtils.isNotEmpty(nodeOperationExecRequest.getParameters())) {
            Map<String, Object> process = Maps.newHashMap();
            Map<String, Object> inputParameterValues = Maps.newHashMap();

            // operation_kwargs --> process --> env
            inputs.put("process", process);
            process.put("env", inputParameterValues);
            if (MapUtils.isNotEmpty(inputParameters)) {
                inputParameterValues.putAll(inputParameters.entrySet().stream()
                        .filter(entry -> !(entry.getValue() instanceof PropertyValue) || ((PropertyValue) entry.getValue()).getValue() != null)
                        .collect(Collectors.toMap(Entry::getKey, entry -> {
                            if (entry.getValue() instanceof PropertyValue) {
                                return ((PropertyValue) entry.getValue()).getValue();
                            } else {
                                return entry.getValue();
                            }
                        })));
            }
            if (MapUtils.isNotEmpty(nodeOperationExecRequest.getParameters())) {
                inputParameterValues.putAll(nodeOperationExecRequest.getParameters());
            }
            // as we do not have the hand on the execute_operation wf, we consider a null parameter value to be an empty string
            replaceNullWithEmptyString(inputParameterValues);
        }
        return workflowParameters;
    }

    /**
     * as we do not have the hand on the execute_operation wf, we consider a null parameter value to be an empty string
     * 
     * @param inputParameterValues the inputs to replace
     */
    private void replaceNullWithEmptyString(Map<String, Object> inputParameterValues) {
        for (Entry<String, Object> paramEntry : inputParameterValues.entrySet()) {
            if (paramEntry.getValue() == null) {
                paramEntry.setValue("");
            }
        }
    }

    public ListenableFuture<Map<String, String>> executeOperation(final CloudifyDeployment deployment,
            final NodeOperationExecRequest nodeOperationExecRequest) {
        BlueprintGenerationUtil util = new BlueprintGenerationUtil(mappingConfigurationHolder.getMappingConfiguration(), deployment,
                blueprintService.resolveBlueprintPath(deployment.getDeploymentPaaSId()), propertyEvaluatorService, deploymentPropertiesService,
                artifactRegistryService);
        if (MapUtils.isEmpty(deployment.getAllNodes()) || !deployment.getAllNodes().containsKey(nodeOperationExecRequest.getNodeTemplateName())) {
            throw new OperationExecutionException("Node " + nodeOperationExecRequest.getNodeTemplateName() + " do not exist in the deployment");
        }
        PaaSNodeTemplate node = deployment.getAllNodes().get(nodeOperationExecRequest.getNodeTemplateName());

        Operation operation = null;
        Map<String, Interface> nodeInterfaces = util.getNonNative().getNodeInterfaces(node);
        if (!MapUtils.isEmpty(nodeInterfaces) && nodeInterfaces.containsKey(nodeOperationExecRequest.getInterfaceName())) {
            Map<String, Operation> interfaceOperations = nodeInterfaces.get(nodeOperationExecRequest.getInterfaceName()).getOperations();
            if (!MapUtils.isEmpty(interfaceOperations) && interfaceOperations.containsKey(nodeOperationExecRequest.getOperationName())) {
                operation = interfaceOperations.get(nodeOperationExecRequest.getOperationName());
            }
        }

        ListenableFuture<Deployment> operationExecutionFuture = waitForExecutionFinish(configurationHolder.getApiClient().getExecutionClient().asyncStart(
                deployment.getDeploymentPaaSId(), Workflow.EXECUTE_OPERATION, buildWorkflowParameters(nodeOperationExecRequest, operation), true, false));
        AsyncFunction<Deployment, Map<String, String>> getOperationResultFunction = input -> {
            ListenableFuture<NodeInstance[]> allInstances = configurationHolder.getApiClient().getNodeInstanceClient()
                    .asyncList(deployment.getDeploymentPaaSId());
            Function<NodeInstance[], Map<String, String>> nodeInstanceToResultFunction = nodeInstances -> {
                Map<String, String> results = Maps.newHashMap();
                for (NodeInstance nodeInstance : nodeInstances) {
                    if (StringUtils.isBlank(nodeOperationExecRequest.getInstanceId())) {
                        if (StringUtils.isNotBlank(nodeOperationExecRequest.getNodeTemplateName())
                                && nodeOperationExecRequest.getNodeTemplateName().equals(nodeInstance.getNodeId())) {
                            results.put(nodeInstance.getId(), fabricMessage(nodeOperationExecRequest, nodeInstance));
                        }
                    } else if (nodeOperationExecRequest.getInstanceId().equals(nodeInstance.getId())) {
                        results.put(nodeInstance.getId(), fabricMessage(nodeOperationExecRequest, nodeInstance));
                    }
                }
                return results;
            };
            return Futures.transform(allInstances, nodeInstanceToResultFunction::apply);
        };
        return Futures.transform(operationExecutionFuture, getOperationResultFunction);
    }

    private String fabricMessage(NodeOperationExecRequest request, NodeInstance nodeInstance) {
        return "Successfully executed " + request.getInterfaceName() + "." + request.getOperationName() + " on instance " + nodeInstance.getId() + " of node "
                + nodeInstance.getNodeId();
    }

    public ListenableFuture scale(String deploymentPaaSId, String nodeId, int delta) {
        Map<String, Object> scaleParameters = Maps.newHashMap();
        scaleParameters.put("node_id", nodeId);
        scaleParameters.put("delta", delta);
        scaleParameters.put("scale_compute", true);
        return waitForExecutionFinish(
                configurationHolder.getApiClient().getExecutionClient().asyncStart(deploymentPaaSId, Workflow.SCALE, scaleParameters, true, false));
    }

    public ListenableFuture launchWorkflow(String deploymentPaaSId, String workflowName, Map<String, Object> workflowParameters) {
        return waitForExecutionFinish(configurationHolder.getApiClient().getExecutionClient().asyncStart(deploymentPaaSId, Workflow.A4C_PREFIX + workflowName,
                workflowParameters, true, false));
    }
}