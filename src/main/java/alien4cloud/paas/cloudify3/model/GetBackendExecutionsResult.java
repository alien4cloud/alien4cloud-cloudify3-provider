package alien4cloud.paas.cloudify3.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Lists;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.UnusedPrivateField")
public class GetBackendExecutionsResult extends AbstractCloudifyModel {

    private Execution[] items;

}
