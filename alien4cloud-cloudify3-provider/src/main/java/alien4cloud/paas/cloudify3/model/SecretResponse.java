package alien4cloud.paas.cloudify3.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecretResponse {
    private String  key;
    private String value;
    @JsonProperty("created_at")
    private Date createdAt;
    @JsonProperty("updated_at")
    private Date updatedAt;
    private String permission;
    @JsonProperty("metadata")
    private String tenantName;
    @JsonProperty("created_by")
    private String createdBy;
    @JsonProperty("resource_availability")
    private String resourceAvailability;
}
