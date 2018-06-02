package alien4cloud.paas.cloudify3.eventpolling;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * This poller is responsible of polling events that have been missed when the system was down.
 */
public class HistoricPoller extends AbstractPoller {

    @Override
    public String getPollerNature() {
        return "Historic stream";
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
