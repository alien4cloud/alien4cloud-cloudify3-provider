package alien4cloud.paas.cloudify3.service.model;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.paas.wf.AbstractStep;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * A sub workflow related to a given host.
 */
@Getter
@Setter
public class HostWorkflow {

    /**
     * The steps related to this host.
     */
    private Map<String, AbstractStep> steps = Maps.newLinkedHashMap();

    /**
     * The link between this host steps (internal links).
     */
    private List<WorkflowStepLink> internalLinks = Lists.newArrayList();

    /**
     * Links that concerns others hosts steps.
     */
    private List<WorkflowStepLink> externalLinks = Lists.newArrayList();

}
