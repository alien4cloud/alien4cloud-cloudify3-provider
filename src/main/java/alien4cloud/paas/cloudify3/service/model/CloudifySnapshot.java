package alien4cloud.paas.cloudify3.service.model;

import alien4cloud.paas.cloudify3.model.Deployment;
import alien4cloud.paas.cloudify3.model.Execution;

import java.util.Map;

/**
 * A cloudify snap: deployments, executions, etc ... all necessary stuff to sync a4c at startup ;)
 */
public class CloudifySnapshot {

    public Deployment[] deployments;

    // deployment.id -> execution
    public Map<String, Execution> executions;

}
