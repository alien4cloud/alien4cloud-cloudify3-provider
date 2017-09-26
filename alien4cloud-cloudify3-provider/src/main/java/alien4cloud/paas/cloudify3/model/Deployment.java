package alien4cloud.paas.cloudify3.model;

import java.util.Date;
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
public class Deployment extends AbstractCloudifyModel {

    private String id;

    private String blueprintId;

    private Date createdAt;

    private Date updatedAt;

    private Workflow[] workflows;

    private Map<String, Object> inputs;

    private Map<String, Object> outputs;

    private Map<String, Object> policyTriggers;

    private Map<String, Object> groups;

    private Map<String, Object> policyTypes;
}
