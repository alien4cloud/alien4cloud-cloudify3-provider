package alien4cloud.paas.cloudify3.event;

import lombok.Getter;

@Getter
public class LiverPollerStarted extends CloudifyManagerEvent  {

    private static final long serialVersionUID = -1126617350064097857L;

    public LiverPollerStarted(Object source) {
        super(source);
    }
}
