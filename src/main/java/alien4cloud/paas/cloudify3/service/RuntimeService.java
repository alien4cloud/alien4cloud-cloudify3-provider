package alien4cloud.paas.cloudify3.service;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

import alien4cloud.paas.cloudify3.model.Deployment;
import alien4cloud.paas.cloudify3.model.Execution;
import alien4cloud.paas.cloudify3.model.ExecutionStatus;
import alien4cloud.paas.cloudify3.model.NodeInstance;
import alien4cloud.paas.cloudify3.restclient.DeploymentClient;
import alien4cloud.paas.cloudify3.restclient.ExecutionClient;
import alien4cloud.paas.cloudify3.restclient.NodeInstanceClient;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class to manage deployment's runtime.
 */
@Slf4j
public abstract class RuntimeService {

    public static final String CLOUDIFY_USER_KEY = "cloudify_user";
    public static final String CLOUDIFY_PASS_KEY = "cloudify_password";
    public static final String CLOUDIFY_TOKEN_KEY = "cloudify_token";

    @Resource
    private NodeInstanceClient nodeInstanceClient;

    @Resource
    protected ExecutionClient executionClient;

    @Resource
    protected DeploymentClient deploymentClient;

    @Resource
    protected ListeningScheduledExecutorService scheduledExecutorService;

    private ListenableFuture<Execution> internalWaitForExecutionFinish(final ListenableFuture<Execution> futureExecution) {
        AsyncFunction<Execution, Execution> waitFunc = new AsyncFunction<Execution, Execution>() {
            @Override
            public ListenableFuture<Execution> apply(final Execution execution) throws Exception {
                if (ExecutionStatus.isTerminated(execution.getStatus())) {
                    log.info("Execution {} for workflow {} has finished with status {}", execution.getId(), execution.getWorkflowId(), execution.getStatus());
                    return futureExecution;
                } else {
                    // If it's not finished, schedule another poll in 2 seconds
                    ListenableFuture<Execution> newFutureExecution = Futures
                            .dereference(scheduledExecutorService.schedule(new Callable<ListenableFuture<Execution>>() {
                                @Override
                                public ListenableFuture<Execution> call() throws Exception {
                                    return executionClient.asyncRead(execution.getId());
                                }
                            }, 2, TimeUnit.SECONDS));
                    return internalWaitForExecutionFinish(newFutureExecution);
                }
            }
        };
        return Futures.transform(futureExecution, waitFunc);
    }

    /**
     * This methods uses Futures.transform to recursively check that the execution provided as a parameter is completed (by calling cloudify rest api) before
     * returning it.
     *
     * @param futureExecution The future execution that has been triggered but may not be completed.
     * @return The future execution that is now completed.
     */
    protected ListenableFuture<Deployment> waitForExecutionFinish(final ListenableFuture<Execution> futureExecution) {
        ListenableFuture<Execution> finishedExecution = internalWaitForExecutionFinish(futureExecution);
        final AsyncFunction<Execution, Deployment> waitSystemWorkflow = new AsyncFunction<Execution, Deployment>() {
            @Override
            public ListenableFuture<Deployment> apply(Execution execution) throws Exception {
                return waitForDeploymentExecutionsFinish(deploymentClient.asyncRead(execution.getDeploymentId()));
            }
        };
        return Futures.transform(finishedExecution, waitSystemWorkflow);
    }

    /**
     * Return a function that waits recursively until the deployment is created in cloudify (All pending executions to create the deployment are indeed
     * completed).
     *
     * @return An AsyncFunction that will return the Deployment only after all pending executions to create the deployment are completed.
     */
    protected ListenableFuture<Deployment> waitForDeploymentExecutionsFinish(final ListenableFuture<Deployment> futureDeployment) {
        if (log.isDebugEnabled()) {
            log.debug("Begin waiting for all executions finished for deployment");
        }
        AsyncFunction<Deployment, Deployment> waitFunc = new AsyncFunction<Deployment, Deployment>() {
            @Override
            public ListenableFuture<Deployment> apply(final Deployment deployment) throws Exception {
                final ListenableFuture<Execution[]> futureExecutions = waitForDeploymentExecutionsFinish(deployment.getId(),
                        executionClient.asyncList(deployment.getId(), true));
                Function<Execution[], Deployment> adaptFunc = new Function<Execution[], Deployment>() {
                    @Override
                    public Deployment apply(Execution[] input) {
                        log.info("All execution has finished for deployment {}", deployment.getId());
                        return deployment;
                    }
                };
                return Futures.transform(futureExecutions, adaptFunc);
            }
        };
        return Futures.transform(futureDeployment, waitFunc);
    }

    private ListenableFuture<Execution[]> waitForDeploymentExecutionsFinish(final String deploymentId, final ListenableFuture<Execution[]> futureExecutions) {
        AsyncFunction<Execution[], Execution[]> waitFunc = new AsyncFunction<Execution[], Execution[]>() {
            @Override
            public ListenableFuture<Execution[]> apply(final Execution[] executions) throws Exception {
                boolean allExecutionFinished = true;
                if (log.isDebugEnabled()) {
                    log.debug("Deployment {} has {} executions", deploymentId, executions.length);
                }
                for (Execution execution : executions) {
                    if (!ExecutionStatus.isTerminated(execution.getStatus())) {
                        allExecutionFinished = false;
                        if (log.isDebugEnabled()) {
                            log.debug("Execution {} for deployment {} has not terminated {}", execution.getWorkflowId(), execution.getDeploymentId(),
                                    execution.getStatus());
                        }
                        break;
                    } else if (log.isDebugEnabled()) {
                        log.debug("Execution {} for deployment {} has terminated {}", execution.getWorkflowId(), execution.getDeploymentId(),
                                execution.getStatus());
                    }
                }
                if (allExecutionFinished && executions.length > 0) {
                    return futureExecutions;
                } else {
                    // If it's not finished, schedule another poll in 2 seconds
                    ListenableFuture<Execution[]> newFutureExecutions = Futures
                            .dereference(scheduledExecutorService.schedule(new Callable<ListenableFuture<Execution[]>>() {
                                @Override
                                public ListenableFuture<Execution[]> call() throws Exception {
                                    return executionClient.asyncList(deploymentId, true);
                                }
                            }, 2, TimeUnit.SECONDS));
                    return waitForDeploymentExecutionsFinish(deploymentId, newFutureExecutions);
                }
            }
        };
        return Futures.transform(futureExecutions, waitFunc);
    }

    protected ListenableFuture<NodeInstance[]> cancelAllRunningExecutions(final String deploymentPaaSId) {
        ListenableFuture<Execution[]> currentExecutions = executionClient.asyncList(deploymentPaaSId, true);
        AsyncFunction<Execution[], ?> abortCurrentExecutionsFunction = new AsyncFunction<Execution[], List<Object>>() {

            @Override
            public ListenableFuture<List<Object>> apply(Execution[] executions) throws Exception {
                List<ListenableFuture<?>> abortExecutions = Lists.newArrayList();
                for (Execution execution : executions) {
                    if (!ExecutionStatus.isTerminated(execution.getStatus())) {
                        if (!execution.getIsSystemWorkflow()) {
                            log.info("Cancel running user workflow execution " + execution.getWorkflowId());
                            abortExecutions.add(waitForExecutionFinish(executionClient.asyncCancel(execution.getId(), true)));
                        } else {
                            log.info("Wait for system execution finished " + execution.getWorkflowId());
                            abortExecutions.add(waitForExecutionFinish(Futures.immediateFuture(execution)));
                        }
                    }
                }
                return Futures.allAsList(abortExecutions);
            }
        };
        ListenableFuture<?> abortCurrentExecutionsFuture = Futures.transform(currentExecutions, abortCurrentExecutionsFunction);
        AsyncFunction<Object, NodeInstance[]> livingNodesRetrievalFunction = new AsyncFunction<Object, NodeInstance[]>() {
            @Override
            public ListenableFuture<NodeInstance[]> apply(Object input) throws Exception {
                return nodeInstanceClient.asyncList(deploymentPaaSId);
            }
        };
        return Futures.transform(abortCurrentExecutionsFuture, livingNodesRetrievalFunction);
    }
}
