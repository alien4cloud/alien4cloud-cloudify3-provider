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
public class Workflow extends AbstractCloudifyModel {

    public static final String A4C_PREFIX = "a4c_";

    public static final String INSTALL = A4C_PREFIX + "install";

    public static final String UNINSTALL = A4C_PREFIX + "uninstall";

    public static final String EXECUTE_OPERATION = "execute_operation";

    public static final String CREATE_DEPLOYMENT_ENVIRONMENT = "create_deployment_environment";

    public static final String DELETE_DEPLOYMENT_ENVIRONMENT = "delete_deployment_environment";

    public static final String UPDATE_DEPLOYMENT = "update";

    public static final String SCALE = A4C_PREFIX + "scale";

    private Date createdAt;

    private String name;

    private Map<String, Object> parameters;
}
