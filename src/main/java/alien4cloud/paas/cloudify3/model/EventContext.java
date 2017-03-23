package alien4cloud.paas.cloudify3.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import alien4cloud.paas.cloudify3.util.EventUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.UnusedPrivateField")
public class EventContext extends AbstractCloudifyModel {

    @Deprecated
    private String taskId;
    @Deprecated
    private String blueprintId;
    @Deprecated
    private String plugin;
    @Deprecated
    private String taskTarget;
    @Deprecated
    private String nodeName;
    @Deprecated
    private String workflowId;
    @Deprecated
    private String taskName;

    private String nodeId;

    private String operation;

    private String deploymentId;

    @Deprecated
    // FIXME:Event4.0 hack due to nodeName not returned in cfy 4.0 event. delete his when fixed
    public String getNodeName() {
        return nodeName != null ? nodeName : EventUtil.extractNodeName(nodeId);
    }
}
