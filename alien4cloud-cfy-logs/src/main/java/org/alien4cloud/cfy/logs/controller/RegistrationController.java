package org.alien4cloud.cfy.logs.controller;

import java.io.IOException;

import javax.annotation.Resource;

import org.alien4cloud.cfy.logs.services.RegistrationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/registration")
public class RegistrationController {
    @Resource
    private RegistrationService registrationService;

    @ResponseBody
    @RequestMapping(method = { RequestMethod.POST }, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String post() throws IOException {
        return registrationService.register();
    }

    @ResponseBody
    @RequestMapping(path = "/{id}", method = { RequestMethod.DELETE }, produces = MediaType.APPLICATION_JSON_VALUE)
    public void delete(@PathVariable String id) throws IOException {
        registrationService.unRegister(id);
    }
}
