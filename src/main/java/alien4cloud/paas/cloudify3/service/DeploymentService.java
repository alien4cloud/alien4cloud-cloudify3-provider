package alien4cloud.paas.cloudify3.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import alien4cloud.deployment.DeploymentLoggingService;
import alien4cloud.paas.cloudify3.configuration.CfyConnectionManager;
import alien4cloud.paas.cloudify3.error.CloudifyAPIException;
import alien4cloud.paas.cloudify3.model.Blueprint;
import alien4cloud.paas.cloudify3.model.Deployment;
import alien4cloud.paas.cloudify3.model.Execution;
import alien4cloud.paas.cloudify3.model.NodeInstance;
import alien4cloud.paas.cloudify3.model.Token;
import alien4cloud.paas.cloudify3.model.Workflow;
import alien4cloud.paas.cloudify3.service.model.CloudifyDeployment;
import alien4cloud.paas.exception.PaaSNotYetDeployedException;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.PaaSDeploymentContext;
import alien4cloud.paas.model.PaaSDeploymentLog;
import alien4cloud.paas.model.PaaSDeploymentLogLevel;
import lombok.extern.slf4j.Slf4j;

/**
 * Handle deployment of the topology on cloudify 3. This service handle from the creation of blueprint from alien model to execution of default workflow
 * install, uninstall.
 */
@Component("cloudify-deployment-service")
@Slf4j
public class DeploymentService extends RuntimeService {
    @Resource
    private BlueprintService blueprintService;
    @Resource
    private StatusService statusService;
    @Inject
    private CfyConnectionManager configurationHolder;
    @Inject
    private DeploymentLoggingService deploymentLoggingService;

    /**
     * Deploy a topology to cloudify.
     * <ul>
     * <li>Map the "DeploymentPaaSId" used as cloudify 'Blueprint Id' to identify the deployment to alien's "DeploymentId" used to identify the deployment in
     * a4c.</li>
     * <li>Generate a cloudify blueprint from the topology</li>
     * <li>Save (Create) the blueprint into cloudify so it is available for deployment (using rest api)</li>
     * <li>Create a deployment.</li>
     * <li>Trigger the install workflow.</li>
     * </ul>
     *
     * @param alienDeployment
     *            The deployment information based on the a4c topology.
     * @return A future linked to the install workflow completed execution.
     */
    public ListenableFuture<Deployment> deploy(final CloudifyDeployment alienDeployment) {
        // generate the blueprint and return in case of failure.
        Path blueprintPath;
        try {
            blueprintPath = blueprintService.generateBlueprint(alienDeployment);
        } catch (IOException e) {
            log.error("Unable to generate the blueprint for " + alienDeployment.getDeploymentPaaSId() + " with alien deployment id "
                    + alienDeployment.getDeploymentId(), e);

            statusService.registerDeploymentStatus(alienDeployment.getDeploymentPaaSId(), DeploymentStatus.FAILURE);
            return Futures.immediateFailedFuture(e);
        }

        // Note: The following code is asynchronous and uses Google Guava Futures to chain (using Futures.transform) several operations.
        // Each operation is triggered once the previous one has been completed and use the result of the previous operation as a parameter for the next one.

        // Save the blueprint in Cloudify catalog so it is available for deployment.
        ListenableFuture<Blueprint> createdBlueprint = configurationHolder.getApiClient().getBlueprintClient()
                .asyncCreate(alienDeployment.getDeploymentPaaSId(), blueprintPath.toString());
        // Create the deployment in cloudify - result doesn't guarantee that the deployment is created but that the deployment is being created (creating) by
        // cloudify. And then wait for completion by polling.
        ListenableFuture<Deployment> creatingDeployment = Futures.transform(createdBlueprint,
                createDeploymentFunction(alienDeployment.getDeploymentPaaSId(), Maps.<String, Object> newHashMap()));
        // Schedule status polling once the deployment created
        Futures.addCallback(creatingDeployment, new FutureCallback<Deployment>() {
            @Override
            public void onSuccess(Deployment result) {
                log.info("Successfully created the deployment {}, begin to poll for status", alienDeployment.getDeploymentPaaSId());
                statusService.scheduleRefreshStatus(alienDeployment.getDeploymentPaaSId());
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Failed to create deployment", t);
            }
        });
        ListenableFuture<Deployment> createdDeployment = waitForDeploymentExecutionsFinish(creatingDeployment);

        // Trigger the install workflow and then wait until the install workflow is completed
        AsyncFunction<Deployment, Execution> installExecutionFunction = installExecutionFunction(alienDeployment.getDeploymentPaaSId());
        ListenableFuture<Execution> installingExecution = Futures.transform(createdDeployment, installExecutionFunction);
        ListenableFuture<Deployment> installedExecution = waitForExecutionFinish(installingExecution);

        // Add a callback to handled failures and provide alien with the correct events.
        addFailureCallback(installedExecution, "Deployment", alienDeployment.getDeploymentPaaSId(), alienDeployment.getDeploymentId(),
                DeploymentStatus.FAILURE);
        return installedExecution;
    }

    public ListenableFuture<Void> update(final CloudifyDeployment alienDeployment) {
        // generate the blueprint and return in case of failure.
        Path blueprintPath;
        try {
            blueprintPath = blueprintService.generateBlueprint(alienDeployment);
        } catch (IOException e) {
            log.error("Unable to generate the blueprint for " + alienDeployment.getDeploymentPaaSId() + " with alien deployment id "
                    + alienDeployment.getDeploymentId(), e);

            statusService.registerDeploymentStatus(alienDeployment.getDeploymentPaaSId(), DeploymentStatus.FAILURE);
            return Futures.immediateFailedFuture(e);
        }

        // update the deployment.
        ListenableFuture<Void> updatingDeployment = configurationHolder.getApiClient().getDeploymentUpdateClient()
                .asyncUpdate(alienDeployment.getDeploymentPaaSId(), blueprintPath.toString());

        // Add a callback to handled failures and provide alien with the correct events.
        addFailureCallback(updatingDeployment, "Update", alienDeployment.getDeploymentPaaSId(), alienDeployment.getDeploymentId(),
                DeploymentStatus.UPDATE_FAILURE);

        return updatingDeployment;
    }

    /**
     * Wraps the deployment client asyncCreate operation into an AsyncFunction so it can be chained using Futures.transform and uses the Blueprint as a
     * parameter once available.
     */
    private AsyncFunction<Blueprint, Deployment> createDeploymentFunction(final String id, final Map<String, Object> inputs) {
        return blueprint -> configurationHolder.getApiClient().getDeploymentClient().asyncCreate(id, blueprint.getId(), inputs);
    }

    /**
     * Wraps the deployment client asyncCreate operation into an AsyncFunction so it can be chained using Futures.transform and uses the Blueprint as a
     * parameter once available.
     */
    private AsyncFunction<Deployment, Execution> installExecutionFunction(final String paasDeploymentId) {
        return deployment -> {
            // now that the create_deployment_environment has been terminated we switch to DEPLOYMENT_IN_PROGRESS state
            // so from now, undeployment is possible
            Map<String, Object> installParameters = Maps.newHashMap();
            Token token = configurationHolder.getApiClient().getTokenClient().get();
            installParameters.put(CLOUDIFY_TOKEN_KEY, token.getValue());
            statusService.registerDeploymentStatus(paasDeploymentId, DeploymentStatus.DEPLOYMENT_IN_PROGRESS);
            return configurationHolder.getApiClient().getExecutionClient().asyncStart(deployment.getId(), Workflow.INSTALL, installParameters, false, false);
        };
    }

    /**
     * Undeploy an application from cloudify.
     * <ul>
     * <li>Delete alien generated blueprint from the local filesystem.</li>
     * <li>Delete blueprint from cloudify catalog.</li>
     * <li>Delete blueprint from cloudify catalog.</li>
     * </ul>
     *
     * @param deploymentContext
     * @return
     */
    public ListenableFuture<?> undeploy(final PaaSDeploymentContext deploymentContext) {
        DeploymentStatus currentStatus = statusService.getFreshStatus(deploymentContext.getDeploymentPaaSId());

        // we shouldn't trigger undeployment if it's in its init stage
        if (DeploymentStatus.INIT_DEPLOYMENT.equals(currentStatus)) {
            return Futures.immediateFailedFuture(new PaaSNotYetDeployedException("Deployment " + deploymentContext.getDeploymentPaaSId()
                    + " is not yet deploy nor in progess so you can't undeploy it in this early stage"));
        }

        // check that the application is not already undeployed
        if (DeploymentStatus.UNDEPLOYED.equals(currentStatus) || DeploymentStatus.UNDEPLOYMENT_IN_PROGRESS.equals(currentStatus)) {
            log.info("Deployment " + deploymentContext.getDeploymentPaaSId() + " has already been undeployed");
            return Futures.immediateFuture(null);
        }

        // start undeployment process and update alien status.
        log.info("Undeploying recipe {} with alien's deployment id {}", deploymentContext.getDeploymentPaaSId(), deploymentContext.getDeploymentId());
        statusService.registerDeploymentStatus(deploymentContext.getDeploymentPaaSId(), DeploymentStatus.UNDEPLOYMENT_IN_PROGRESS);

        // Remove blueprint from the file system (alien) - we keep it for debuging perspective as long as deployment is up.
        blueprintService.deleteBlueprint(deploymentContext.getDeploymentPaaSId());

        // Cancel all running executions for this deployment.
        ListenableFuture<NodeInstance[]> cancelRunningExecutionsFuture = cancelAllRunningExecutions(deploymentContext.getDeploymentPaaSId());
        // Once all executions are cancelled we start the un-deployment workflow.
        AsyncFunction<NodeInstance[], Deployment> uninstallFunction = uninstallFunction(deploymentContext);
        ListenableFuture<Deployment> uninstalled = Futures.transform(cancelRunningExecutionsFuture, uninstallFunction);
        // Delete the deployment from cloudify.
        ListenableFuture<?> deletedDeployment = Futures.transform(uninstalled, deleteDeploymentFunction(deploymentContext));
        // Delete the blueprint from cloudify.
        ListenableFuture<?> undeploymentFuture = Futures.transform(deletedDeployment, deleteBlueprintFunction(deploymentContext));

        // Add a callback to handled failures and provide alien with the correct events.
        // TODO should we check the status of the deployment before we mark it as undeployed ?
        addFailureCallback(undeploymentFuture, "Undeployment", deploymentContext.getDeploymentPaaSId(), deploymentContext.getDeploymentId(), null);
        return undeploymentFuture;
    }

    private AsyncFunction<NodeInstance[], Deployment> uninstallFunction(final PaaSDeploymentContext deploymentContext) {
        return livingNodes -> {
            if (livingNodes != null && livingNodes.length > 0) {
                // trigger the uninstall workflow only if there is some node instances.
                Map<String, Object> uninstallParameters = Maps.newHashMap();
                Token token = configurationHolder.getApiClient().getTokenClient().get();
                uninstallParameters.put(CLOUDIFY_TOKEN_KEY, token.getValue());
                ListenableFuture<Execution> triggeredUninstallWorkflow = configurationHolder.getApiClient().getExecutionClient()
                        .asyncStart(deploymentContext.getDeploymentPaaSId(), Workflow.UNINSTALL, uninstallParameters, false, false);
                // ensure that the workflow execution is finished.
                return waitForExecutionFinish(triggeredUninstallWorkflow);
            } else {
                return Futures.immediateFuture(null);
            }
        };
    }

    private AsyncFunction<Deployment, Deployment> deleteDeploymentFunction(final PaaSDeploymentContext deploymentContext) {
        return deployment -> {
            // TODO Due to bug index not refreshed of cloudify 3.1 (will be corrected in 3.2). We schedule the delete of deployment 2 seconds after the
            ListenableFuture<?> deleteDeploymentFuture = Futures.dereference(scheduledExecutorService.schedule(
                    () -> configurationHolder.getApiClient().getDeploymentClient().asyncDelete(deploymentContext.getDeploymentPaaSId()), 2, TimeUnit.SECONDS));
            return Futures.transform(deleteDeploymentFuture,
                    (AsyncFunction<Object, Deployment>) input -> waitForDeploymentDeleted(deploymentContext.getDeploymentPaaSId()));
        };
    }

    private AsyncFunction<Object, Object> deleteBlueprintFunction(final PaaSDeploymentContext deploymentContext) {
        return input -> {
            // TODO Due to bug index not refreshed of cloudify 3.1 (will be corrected in 3.2). We schedule the delete of blueprint 2 seconds after the
            // delete of
            // deployment
            return Futures.dereference(scheduledExecutorService.schedule((Callable<ListenableFuture<?>>) () -> configurationHolder.getApiClient()
                    .getBlueprintClient().asyncDelete(deploymentContext.getDeploymentPaaSId()), 2, TimeUnit.SECONDS));
        };
    }

    private void addFailureCallback(ListenableFuture future, final String operationName, final String deploymentPaaSId, final String deploymentId,
            final DeploymentStatus status) {
        Futures.addCallback(future, new FutureCallback() {
            @Override
            public void onSuccess(Object result) {
                log.info(operationName + " of deployment {} with alien's deployment id {} has been executed asynchronously", deploymentPaaSId, deploymentId);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error(operationName + " of deployment " + deploymentPaaSId + " with alien's deployment id " + deploymentId + " has failed", t);
                if (status != null) {
                    // User wants to send back an intermediary status before the real one then sends it
                    statusService.registerDeploymentStatus(deploymentPaaSId, status);
                }
                // save the error as log so that it should appear in the log view
                saveExceptionAsLog(t, operationName, deploymentId, deploymentPaaSId);

                // Send the real status later, status service takes care of not sending anything if the status has not changed
                statusService.registerDeploymentStatus(deploymentPaaSId, statusService.getStatusFromCloudify(deploymentPaaSId));
            }
        });
    }

    private void saveExceptionAsLog(Throwable throwable, String operationName, String deploymentId, String deploymentPaaSId) {
        if (!(throwable instanceof CloudifyAPIException)) {
            return;
        }

        StringBuilder logContentBuilder = new StringBuilder("'" + operationName + "' ");
        logContentBuilder.append(throwable.getMessage());
        PaaSDeploymentLog log = new PaaSDeploymentLog();
        log.setDeploymentId(deploymentId);
        log.setDeploymentPaaSId(deploymentPaaSId);
        log.setContent(logContentBuilder.toString());
        log.setLevel(PaaSDeploymentLogLevel.ERROR);
        log.setTimestamp(new Date());

        deploymentLoggingService.save(log);
    }
}
