package alien4cloud.paas.cloudify3.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.UnusedPrivateField")
public class Node extends AbstractCloudifyModel {

    private String id;

    private String deploymentId;

    private Map<String, Object> properties;

    private String blueprintId;

    private int numberOfInstances;

    private int deployNumberOfInstances;

    private String hostId;

    private Set<String> typeHierarchy;

    private String type;

    private List<Relationship> relationships;
}
