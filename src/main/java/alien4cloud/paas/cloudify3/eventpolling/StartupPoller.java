package alien4cloud.paas.cloudify3.eventpolling;

/**
 * This poller is responsible of polling events that have been missed when the system was down.
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

    @Override
    public void shutdown() {
    }

}
