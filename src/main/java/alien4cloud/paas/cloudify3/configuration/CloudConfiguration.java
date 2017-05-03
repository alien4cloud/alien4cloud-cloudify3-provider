package alien4cloud.paas.cloudify3.configuration;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

import alien4cloud.exception.NotFoundException;
import alien4cloud.ui.form.annotation.FormProperties;
import alien4cloud.ui.form.annotation.FormPropertyConstraint;
import alien4cloud.ui.form.annotation.FormPropertyDefinition;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@FormProperties({ "url", "locations", "userName", "password", "tenant", "disableSSLVerification", "delayBetweenDeploymentStatusPolling",
        "delayBetweenInProgressDeploymentStatusPolling", "disableDiamondMonitorAgent", "kubernetes" })
@EqualsAndHashCode(of = { "url", "userName", "password", "tenant" })
public class CloudConfiguration {

    @FormPropertyConstraint(pattern = "http\\:.+(?:\\d+)")
    @NotNull
    private String url;

    @NotNull
    private String userName;

    @FormPropertyDefinition(type = "string", isPassword = true, isRequired = true)
    private String password;

    @NotNull
    private String tenant;

    @NotNull
    private Boolean disableSSLVerification;

    @NotNull
    private LocationConfigurations locations;

    @NotNull
    private Integer delayBetweenDeploymentStatusPolling;

    @NotNull
    private Integer delayBetweenInProgressDeploymentStatusPolling;

    @NotNull
    private Boolean disableDiamondMonitorAgent = false;

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