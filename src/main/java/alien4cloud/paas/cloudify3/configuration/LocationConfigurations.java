package alien4cloud.paas.cloudify3.configuration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocationConfigurations {

    private LocationConfiguration openstack;

    private LocationConfiguration amazon;

    private LocationConfiguration byon;
}
