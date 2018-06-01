package alien4cloud.paas.cloudify3.eventpolling;

/**
 * Created by xdegenne on 01/06/2018.
 */
public class StartupPoller extends AbstractPoller {

    public StartupPoller(String url) {
        super(url);
    }

    @Override
    public void start() {
        // TODO: get the last event timestamp from A4C ES
//        this.pollEpoch(lastEventTimestamp, now);
    }

}
