package alien4cloud.paas.cloudify3.restclient;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;

import alien4cloud.paas.cloudify3.model.Deployment;
import alien4cloud.paas.cloudify3.model.Event;
import alien4cloud.paas.cloudify3.model.Execution;
import alien4cloud.paas.cloudify3.model.ExecutionStatus;
import alien4cloud.paas.cloudify3.model.Node;
import alien4cloud.paas.cloudify3.model.NodeInstance;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestExecutionClient extends AbstractRestClientTest {

    private static BlueprintClient blueprintClient;

    private static DeploymentClient deploymentClient;

    private static ExecutionClient executionClient;

    private static DeploymentEventClient deploymentEventClient;

    private static NodeClient nodeClient;

    private static NodeInstanceClient nodeInstanceClient;

    @BeforeClass
    public static void before() {
        initializeContext();
        blueprintClient = configureClient(new BlueprintClient());
        deploymentClient = configureClient(new DeploymentClient());
        executionClient = configureClient(new ExecutionClient());
        deploymentEventClient = configureClient(new DeploymentEventClient());
        nodeClient = configureClient(new NodeClient());
        nodeInstanceClient = configureClient(new NodeInstanceClient());
    }

    private static void waitForAllExecutionToFinish(String blueprintId) {
        while (true) {
            Execution[] executions = executionClient.list(blueprintId, true);
            boolean allFinished = true;
            for (Execution execution : executions) {
                if (!ExecutionStatus.isTerminated(execution.getStatus())) {
                    allFinished = false;
                }
            }
            if (allFinished) {
                break;
            } else {
                sleep();
            }
        }
    }

    @Test
    public void testDeployment() {
        String blueprintId = "testRest";
        Deployment[] deployments = deploymentClient.list();
        Assert.assertEquals(0, deployments.length);
        try {
            blueprintClient.create(blueprintId, "src/test/resources/restclient/blueprint/blueprint.yaml");
            deploymentClient.create(blueprintId, blueprintId, Maps.newHashMap());
            deployments = deploymentClient.list();
            Assert.assertEquals(1, deployments.length);
            for (Deployment deployment : deployments) {
                log.info(deployment.getId() + " = " + deployment);
            }
            Assert.assertNotNull(deploymentClient.read(blueprintId));
        } finally {
            waitForAllExecutionToFinish(blueprintId);
            deploymentClient.delete(blueprintId);
            blueprintClient.delete(blueprintId);
        }
    }

    @Test
    public void testExecution() {
        String blueprintId = "testRest";
        try {
            blueprintClient.create(blueprintId, "src/test/resources/restclient/blueprint/blueprint.yaml");
            deploymentClient.create(blueprintId, blueprintId, Maps.newHashMap());
            Execution[] executions = executionClient.list(blueprintId, true);
            sleep();
            Assert.assertEquals(1, executions.length);
            for (Execution execution : executions) {
                log.info(execution.getWorkflowId() + " = " + execution.getIsSystemWorkflow());
            }
            waitForAllExecutionToFinish(blueprintId);
            Execution installExecution = executionClient.start(blueprintId, "install", null, false, false);
            sleep();
            Assert.assertNotNull(executionClient.read(installExecution.getId()));
            waitForAllExecutionToFinish(blueprintId);
            Event[] installEvents = deploymentEventClient.getBatch(installExecution.getId(), installExecution.getCreatedAt(), 0, Integer.MAX_VALUE);
            Assert.assertTrue(installEvents.length > 0);

            Node[] nodes = nodeClient.list(blueprintId, null);
            Assert.assertEquals(1, nodes.length);
            Node node = nodeClient.list(blueprintId, nodes[0].getId())[0];
            Assert.assertNotNull(node);
            Assert.assertNotNull(node.getProperties());
            Assert.assertFalse(node.getProperties().isEmpty());

            NodeInstance[] nodeInstances = nodeInstanceClient.list(blueprintId);
            Assert.assertEquals(1, nodeInstances.length);
            NodeInstance nodeInstance = nodeInstanceClient.read(nodeInstances[0].getId());
            Assert.assertNotNull(nodeInstance);
            Assert.assertNotNull(nodeInstance.getRuntimeProperties());
            Assert.assertFalse(nodeInstance.getRuntimeProperties().isEmpty());
        } finally {
            executionClient.start(blueprintId, "uninstall", null, false, false);
            sleep();
            waitForAllExecutionToFinish(blueprintId);
            deploymentClient.delete(blueprintId);
            blueprintClient.delete(blueprintId);
        }
    }
}
