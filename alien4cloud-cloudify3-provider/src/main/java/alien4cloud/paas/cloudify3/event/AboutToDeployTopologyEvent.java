package alien4cloud.paas.cloudify3.event;

import lombok.Getter;

import org.springframework.context.ApplicationEvent;

import alien4cloud.paas.model.PaaSTopologyDeploymentContext;

@Getter
public class AboutToDeployTopologyEvent extends ApplicationEvent {

    private static final long serialVersionUID = -1126617350064097857L;

    private PaaSTopologyDeploymentContext deploymentContext;

    public AboutToDeployTopologyEvent(Object source, PaaSTopologyDeploymentContext deploymentContext) {
        super(source);
        this.deploymentContext = deploymentContext;
    }

}
