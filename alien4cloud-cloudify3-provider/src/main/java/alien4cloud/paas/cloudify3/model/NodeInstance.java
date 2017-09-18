package alien4cloud.paas.cloudify3.model;

import java.util.List;
import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter
@Setter
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.UnusedPrivateField")
public class NodeInstance extends AbstractCloudifyModel {

    private String id;

    private String nodeId;

    private String hostId;

    private String deploymentId;

    private Map<String, Object> runtimeProperties;

    private String state;

    private List<RelationshipInstance> relationships;
}
