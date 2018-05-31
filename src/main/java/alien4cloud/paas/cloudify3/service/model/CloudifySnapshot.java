package alien4cloud.paas.cloudify3.service.model;

import java.util.List;
import java.util.Map;

import alien4cloud.paas.cloudify3.model.Deployment;
import alien4cloud.paas.cloudify3.model.Execution;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * A cloudify snap: deployments, executions, etc ... all necessary stuff to sync a4c at startup ;)
 */
@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode
public class CloudifySnapshot {

    @NonNull
    public Map<String, Deployment> deployments;

    @NonNull
    // deployment.id -> execution
    public Map<String, List<Execution>> executions;
}
