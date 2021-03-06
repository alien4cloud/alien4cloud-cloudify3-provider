package alien4cloud.paas.cloudify3.restclient;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.paas.cloudify3.model.CloudifyLifeCycle;
import alien4cloud.paas.cloudify3.model.Deployment;
import alien4cloud.paas.cloudify3.model.Event;
import alien4cloud.paas.cloudify3.model.Execution;
import alien4cloud.paas.cloudify3.model.ExecutionStatus;
import alien4cloud.paas.cloudify3.model.Node;
import alien4cloud.paas.cloudify3.model.NodeInstance;
import alien4cloud.paas.cloudify3.shared.restclient.BlueprintClient;
import alien4cloud.paas.cloudify3.shared.restclient.DeploymentClient;
import alien4cloud.paas.cloudify3.shared.restclient.EventClient;
import alien4cloud.paas.cloudify3.shared.restclient.ExecutionClient;
import alien4cloud.paas.cloudify3.shared.restclient.NodeClient;
import alien4cloud.paas.cloudify3.shared.restclient.NodeInstanceClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestExecutionClient extends AbstractRestClientTest {

    private static BlueprintClient blueprintClient;

    private static DeploymentClient deploymentClient;

    private static ExecutionClient executionClient;

    private static EventClient eventClient;

    private static NodeClient nodeClient;

    private static NodeInstanceClient nodeInstanceClient;

    @BeforeClass
    public static void before() {
        initializeContext();
        blueprintClient = getApiClient().getBlueprintClient();
        deploymentClient = getApiClient().getDeploymentClient();
        executionClient = getApiClient().getExecutionClient();
        eventClient = getApiClient().getEventClient();
        nodeClient = getApiClient().getNodeClient();
        nodeInstanceClient = getApiClient().getNodeInstanceClient();
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
    public void testCreateDeployment() {
        String blueprintId = "testRest";
        Deployment[] deployments = deploymentClient.list(0, 1000).getItems();
        Assert.assertEquals(0, deployments.length);
        try {
            blueprintClient.create(blueprintId, "src/test/resources/restclient/blueprint/blueprint.yaml");
            deploymentClient.create(blueprintId, blueprintId, Maps.newHashMap());
            deployments = deploymentClient.list(0, 1000).getItems();
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
    public void testExecution() throws ExecutionException, InterruptedException {
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
            log.info("sleeping...");
            sleep();
            Assert.assertNotNull(executionClient.read(installExecution.getId()));
            waitForAllExecutionToFinish(blueprintId);

            Event[] installEvents = eventClient.asyncGetBatch(installExecution.getCreatedAt(), null, 0, Integer.MAX_VALUE).get();
            Assert.assertTrue(installEvents.length > 0);
            assertLifecycleEvents(installEvents, CloudifyLifeCycle.CREATE, CloudifyLifeCycle.START);

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
            processUninstall(blueprintId);
        }
    }

    private void processUninstall(String blueprintId) throws ExecutionException, InterruptedException {
        Execution uninstallExecution = executionClient.start(blueprintId, "uninstall", null, false, false);
        sleep();
        Assert.assertNotNull(executionClient.read(uninstallExecution.getId()));
        waitForAllExecutionToFinish(blueprintId);
        Event[] events = eventClient.asyncGetBatch(uninstallExecution.getCreatedAt(), null, 0, Integer.MAX_VALUE).get();
        deploymentClient.delete(blueprintId);
        blueprintClient.delete(blueprintId);
        Assert.assertTrue(events.length > 0);
        assertLifecycleEvents(events, CloudifyLifeCycle.STOP, CloudifyLifeCycle.DELETE);
    }

    private void assertLifecycleEvents(Event[] events, String... lifeCycles) {
        // System.out.println(ArrayUtils.toString(events));
        List<Event> list = Lists.newArrayList(events);
        for (String lifeCycle : lifeCycles) {
            int expected = 1;
            long actual = list.stream().filter(event -> Objects.equals(event.getContext().getOperation(), lifeCycle)).count();
            Assert.assertEquals("lifecycle " + lifeCycle + "--> Expected:" + expected + " Actual:" + actual, expected, actual);
        }
    }
}
