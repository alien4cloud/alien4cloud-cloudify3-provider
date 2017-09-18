package org.alien4cloud.cfy.logs.controller;

import java.io.IOException;

import org.alien4cloud.cfy.logs.services.LogRegistration;
import org.alien4cloud.cfy.logs.services.RegistrationService;
import org.elasticsearch.common.inject.Inject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

/**
 * Simple controller that can receive logs from a logstash save them to a H2 db used as a queue and get polled by remote process.
 */
@Slf4j
@RestController
@RequestMapping("/api/logs")
public class LogController {
    @Inject
    private RegistrationService registrationService;

    @ResponseBody
    @RequestMapping(method = { RequestMethod.POST }, consumes = MediaType.APPLICATION_JSON_VALUE)
    public void postLogs(@RequestBody String content) throws IOException {
        // Dispatching logs
        for (LogRegistration registration : registrationService.getRegistrations()) {
            registration.getLogQueue().addLog(content);
        }
    }

    @ResponseBody
    @RequestMapping(path = "/{registrationId}", method = { RequestMethod.GET }, produces = MediaType.APPLICATION_JSON_VALUE)
    public String getLogs(@PathVariable String registrationId) throws IOException {
        return registrationService.getRegistrationOrFail(registrationId).getLogQueue().getLogs();
    }

    @ResponseBody
    @RequestMapping(path = "/{registrationId}/ack/{batchId}", method = { RequestMethod.GET }, produces = MediaType.APPLICATION_JSON_VALUE)
    public void ackLogs(@PathVariable String registrationId, @PathVariable Long batchId) {
        registrationService.getRegistrationOrFail(registrationId).getLogQueue().ackLogs(batchId);
    }
}
