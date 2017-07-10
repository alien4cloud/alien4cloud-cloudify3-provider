package alien4cloud.paas.cloudify3.configuration;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

import alien4cloud.exception.NotFoundException;
import alien4cloud.ui.form.annotation.FormProperties;
import alien4cloud.ui.form.annotation.FormPropertyConstraint;
import alien4cloud.ui.form.annotation.FormPropertyDefinition;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@FormProperties({ "url", "locations", "userName", "password", "disableSSLVerification", "delayBetweenDeploymentStatusPolling",
        "delayBetweenInProgressDeploymentStatusPolling", "connectionTimeout","disableDiamondMonitorAgent", "kubernetes" })
public class CloudConfiguration {

    @FormPropertyConstraint(pattern = "http\\:.+(?:\\d+)")
    @NotNull
    private String url;

    @NotNull
    private LocationConfigurations locations;

    private String userName;

    @FormPropertyDefinition(type = "string", isPassword = true)
    private String password;

    @NotNull
    private Boolean disableSSLVerification;

    @NotNull
    private Integer delayBetweenDeploymentStatusPolling;

    @NotNull
    private Integer delayBetweenInProgressDeploymentStatusPolling;

    @NotNull
    private Boolean disableDiamondMonitorAgent = false;

    private Integer connectionTimeout;

    private KubernetesConfiguration kubernetes;

    @JsonIgnore
    public LocationConfiguration getConfigurationLocation(String locationName) {
        switch (locationName) {
        case "amazon":
            return locations.getAmazon();
        case "openstack":
            return locations.getOpenstack();
        case "byon":
            return locations.getByon();
        }
        throw new NotFoundException("Location " + locationName + " not found");
    }
}
