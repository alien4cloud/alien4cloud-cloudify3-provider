package org.alien4cloud.cfy.logs.services;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import alien4cloud.exception.NotFoundException;
import alien4cloud.utils.FileUtil;

/**
 * Test the registration and lease mechanism.
 */
public class RegistrationTest {
    private static final String REGISTRATION_DIRECTORY = "target/test-data/registrationTest";

    // Test that I expire if I do nothing
    @Test
    public void testExpiration() throws IOException, InterruptedException {
        Path registrationDirectory = Paths.get(REGISTRATION_DIRECTORY);

        // Delete if exist so the test start on clean data.
        if (registrationDirectory.toFile().exists()) {
            FileUtil.delete(registrationDirectory);
        }

        RegistrationService registrationService = new RegistrationService(REGISTRATION_DIRECTORY, true, 5, 5, 10, 100);
        registrationService.init();
        // Register
        String registrationId = registrationService.register();
        // Assert that the registration exists
        Assert.assertEquals(1, registrationService.getRegistrations().size());
        Assert.assertNotNull(registrationService.getRegistrationOrFail(registrationId));
        Thread.sleep(10 * 1000);
        Assert.assertEquals(0, registrationService.getRegistrations().size());
        try {
            registrationService.getRegistrationOrFail(registrationId);
            Assert.fail("Registration should have been removed and not found exception has to be thrown");
        } catch (NotFoundException e) {
            Assert.assertNotNull(e);
        }
        registrationService.destroy();
    }

    // Test that I don't expire as long as I am active and that I expire after that
    @Test
    public void testNotExpireWhileActive() throws InterruptedException, IOException {
        Path registrationDirectory = Paths.get(REGISTRATION_DIRECTORY);

        // Delete if exist so the test start on clean data.
        if (registrationDirectory.toFile().exists()) {
            FileUtil.delete(registrationDirectory);
        }

        RegistrationService registrationService = new RegistrationService(REGISTRATION_DIRECTORY, true, 5, 5, 10, 100);
        registrationService.init();
        // Register
        String registrationId = registrationService.register();
        // Assert that the registration exists
        Assert.assertEquals(1, registrationService.getRegistrations().size());
        Assert.assertNotNull(registrationService.getRegistrationOrFail(registrationId));
        long duration = System.currentTimeMillis() + 10 * 1000;
        while (System.currentTimeMillis() < duration) {
            registrationService.getRegistrationOrFail(registrationId);
            Thread.sleep(100);
        }
        Assert.assertEquals(1, registrationService.getRegistrations().size());
        Assert.assertNotNull(registrationService.getRegistrationOrFail(registrationId));
        registrationService.destroy();
    }

    // Test that there is no expiration if set to inactive
    @Test
    public void testNotExpireIfNotLeased() throws InterruptedException, IOException {
        Path registrationDirectory = Paths.get(REGISTRATION_DIRECTORY);

        // Delete if exist so the test start on clean data.
        if (registrationDirectory.toFile().exists()) {
            FileUtil.delete(registrationDirectory);
        }

        RegistrationService registrationService = new RegistrationService(REGISTRATION_DIRECTORY, false, 5, 5, 10, 100);
        registrationService.init();
        // Register
        String registrationId = registrationService.register();
        // Assert that the registration exists
        Assert.assertEquals(1, registrationService.getRegistrations().size());
        Assert.assertNotNull(registrationService.getRegistrationOrFail(registrationId));
        Thread.sleep(10 * 1000);
        Assert.assertEquals(1, registrationService.getRegistrations().size());
        Assert.assertNotNull(registrationService.getRegistrationOrFail(registrationId));
        registrationService.destroy();
    }

    @Test
    public void testInitFromExisting() throws IOException {
        Path registrationDirectory = Paths.get(REGISTRATION_DIRECTORY);

        // Delete if exist so the test start on clean data.
        if (registrationDirectory.toFile().exists()) {
            FileUtil.delete(registrationDirectory);
        }

        FileUtil.copy(Paths.get("src/test/resources/data/registration"), registrationDirectory);

        RegistrationService registrationService = new RegistrationService(REGISTRATION_DIRECTORY, false, 5, 5, 10, 100);
        registrationService.init();
        Assert.assertNotNull(registrationService.getRegistrationOrFail("c66474f8-eb0a-4f0a-a33c-c92d1bc70a06"));
    }
}