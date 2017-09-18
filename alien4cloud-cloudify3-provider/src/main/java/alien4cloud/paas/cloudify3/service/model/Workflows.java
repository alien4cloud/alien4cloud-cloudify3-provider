package alien4cloud.paas.cloudify3.service.model;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.paas.wf.Workflow;

import com.google.common.collect.Maps;

@Getter
@Setter
public class Workflows {

    private Map<String, Workflow> workflows;

    /**
     * The install workflow steps by host.
     * <ul>
     * <li>key is the host
     * <li>value is a sub-workflow related to the given host
     * </ul>
     */
    private Map<String, HostWorkflow> installHostWorkflows = Maps.newLinkedHashMap();

    /**
     * The uninstall workflow steps by host.
     * <ul>
     * <li>key is the host
     * <li>value is a sub-workflow related to the given host
     * </ul>
     */
    private Map<String, HostWorkflow> uninstallHostWorkflows = Maps.newLinkedHashMap();

    /**
     * Per standard workflow, orphan steps and external links
     */
    private Map<String, StandardWorkflow> standardWorkflows = Maps.newLinkedHashMap();

}
