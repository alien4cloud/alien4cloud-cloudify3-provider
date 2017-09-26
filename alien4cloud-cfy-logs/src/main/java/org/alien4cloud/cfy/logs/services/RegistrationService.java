package org.alien4cloud.cfy.logs.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;

/**
 * Manage registrations.
 */
@Slf4j
@Component
public class RegistrationService {
    private ObjectMapper mapper = new ObjectMapper();
    private Path rootPath;
    private Path registrationFilePath;

    /** If false then registrations are never expired. */
    private boolean isLeased;
    /** Lease of the registrations. It is not persisted, if the server is down then all registrations will be renewed at startup. */
    private long registrationLeaseMillis;
    /** The thread responsible for checking leases and expiration. Note that the thread exists only when isLeased is true. */
    private Thread leaseManagerThread;
    /** The frequency on which the lease manager is checking expirations. */
    private long leaseManagerFrequencyMillis;
    /** Flag to stop the lease manager */
    private boolean stopLeaseManager;

    private long flushTimeoutSeconds;
    private int flushBatchSize;

    private Map<String, LogRegistration> logRegistrations = Maps.newHashMap();

    public RegistrationService(@Value("${home_directory}") String path, @Value("${alien_registration.is_leased}") boolean isLeased,
            @Value("${alien_registration.lease_duration_sec}") long registrationLeaseSeconds,
            @Value("${alien_registration.lease_expiration_frequency_sec}") long leaseManagerFrequencySeconds,
            @Value("${buffer.timeout_sec}") long flushTimeoutSeconds, @Value("${buffer.size}") int flushBatchSize

    ) throws IOException {
        rootPath = Paths.get(path).toAbsolutePath();

        registrationFilePath = rootPath.resolve("registrations.json");

        if (!Files.isDirectory(rootPath)) {
            Files.createDirectories(rootPath);
            log.info("Cfy to alien log server directory created at " + rootPath.toAbsolutePath());
        } else {
            log.info("Cfy to alien log server directory already created at " + rootPath.toAbsolutePath());
        }

        this.isLeased = isLeased;
        this.registrationLeaseMillis = registrationLeaseSeconds * 1000;
        this.leaseManagerFrequencyMillis = leaseManagerFrequencySeconds * 1000;

        this.flushBatchSize = flushBatchSize;
        this.flushTimeoutSeconds = flushTimeoutSeconds;
    }

    public synchronized String register() throws IOException {
        String registrationId = UUID.randomUUID().toString();

        addFromId(registrationId);

        // save json registration file
        mapper.writeValue(registrationFilePath.toFile(), logRegistrations.keySet());

        return registrationId;
    }

    public synchronized void unRegister(String registrationId) throws IOException {
        LogRegistration expired = logRegistrations.remove(registrationId);
        if (expired != null) {
            log.info("Un-register registration <" + registrationId + ">");
            expired.getLogQueue().remove();
        }
    }

    private void addFromId(String registrationId) throws IOException {
        log.info("Register a new alien4cloud client with id <" + registrationId + ">");
        LogRegistration logRegistration = new LogRegistration(registrationId, getExpirationDate(),
                new LogQueue(registrationId, rootPath.resolve(registrationId), flushTimeoutSeconds, flushBatchSize));
        logRegistrations.put(registrationId, logRegistration);
    }

    public Collection<LogRegistration> getRegistrations() {
        return logRegistrations.values();
    }

    public synchronized LogRegistration getRegistrationOrFail(String registrationId) {
        LogRegistration registration = logRegistrations.get(registrationId);
        if (registration == null) {
            throw new NotFoundException("No registration can be found for id " + registrationId);
        }
        registration.setExpirationDate(getExpirationDate());
        return registration;
    }

    private synchronized void expire() throws IOException {
        long currentDate = new Date().getTime();
        Set<String> expiredIds = Sets.newHashSet();
        for (LogRegistration logRegistration : logRegistrations.values()) {
            if (currentDate > logRegistration.getExpirationDate()) {
                expiredIds.add(logRegistration.getId());
            }
        }
        for (String expiredId : expiredIds) {
            log.info("Registration <" + expiredId + "> expired");
            unRegister(expiredId);
        }
    }

    private long getExpirationDate() {
        return new Date().getTime() + registrationLeaseMillis;
    }

    @PostConstruct
    public void init() throws IOException {
        // Load existing registrations
        File f = registrationFilePath.toFile();
        if (f.exists()) {
            List<String> registrationIds = mapper.readValue(f, new TypeReference<List<String>>() {
            });
            for (String registrationId : registrationIds) {
                addFromId(registrationId);
            }
        }

        // Start the expiration thread if expiration is on.
        if (isLeased) {
            log.info("Server is configured to expire alien registrations after " + registrationLeaseMillis / 1000 + " seconds");
            stopLeaseManager = false;
            // Launch the expiration thread.
            leaseManagerThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (!stopLeaseManager) {
                            // run expiration logic
                            expire();
                            Thread.sleep(leaseManagerFrequencyMillis);
                        }
                    } catch (InterruptedException e) {
                        log.warn("Stopping registration expiration thread as an interupted exception has been caught");
                    } catch (IOException e) {
                        log.error("IO error is stopping expiration thread.");
                    }
                }
            });
            leaseManagerThread.setName("registration_expiration");
            leaseManagerThread.start();
        } else {
            log.info("Server is configured so that alien registration never expires.");
        }
    }

    @PreDestroy
    public void destroy() {
        stopLeaseManager = true;
        if (isLeased) {
            leaseManagerThread.interrupt();
        }
    }
}
