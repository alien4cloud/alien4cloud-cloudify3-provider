package org.alien4cloud.cfy.logs.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import alien4cloud.utils.AlienUtils;
import alien4cloud.utils.FileUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * A Log queue manager.
 */
@Slf4j
public class LogQueue {
    private String registrationId;
    /** The path of the root directory in which log files are stored. */
    private Path registrationPath;
    /** Id of the next batch to deliver */
    private long batchToDeliver;
    /** The current file id. */
    private long batchIncrement;
    private boolean active = true;

    private ScheduledExecutorService executor;
    private ScheduledFuture nextFlush;

    private int flushBatchSize;
    private long flushTimeoutSeconds;

    private StringBuilder sb = null;
    private int currentBatchSize = 0;

    public LogQueue(String registrationId, Path registrationPath, long flushTimeoutSeconds, int flushBatchSize) throws IOException {
        this.registrationId = registrationId;
        this.registrationPath = registrationPath;
        this.flushTimeoutSeconds = flushTimeoutSeconds;
        this.flushBatchSize = flushBatchSize;

        batchIncrement = 0;
        batchToDeliver = Long.MAX_VALUE;

        // Init directory if it do not exists
        if (!Files.isDirectory(registrationPath)) {
            Files.createDirectories(registrationPath);
            log.info("Init queue for registration " + registrationId + " using path " + registrationPath.toAbsolutePath());
        } else {
            // If exists then load existing files to get the next-batch id and current increment state.
            try (Stream<Path> stream = Files.list(registrationPath)) {
                stream.map(Path::getFileName).forEach(fileName -> {
                    long fileIndex = Long.valueOf(FilenameUtils.removeExtension(fileName.toString()));
                    batchIncrement = fileIndex > batchIncrement ? fileIndex : batchIncrement;
                    batchToDeliver = fileIndex < batchToDeliver ? fileIndex : batchToDeliver;
                });
            }
            batchIncrement++;

            log.info("Init queue from existing data at <" + registrationPath.toAbsolutePath() + ">");
        }
        if (batchToDeliver == Long.MAX_VALUE) {
            batchToDeliver = -1;
        }
        log.info("Initialized with next batch to deliver: <" + batchToDeliver + ">  next batch index: <" + batchIncrement + ">");

        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("log-queue-" + registrationId + "-%d").build();
        executor = Executors.newSingleThreadScheduledExecutor(namedThreadFactory);
        scheduleFlush();
    }

    public synchronized void addLog(String logString) {
        if (!active) {
            log.warn("Log queue for registration <" + registrationId + "> is not active anymore. Logs will be dropped.");
            return;
        }
        if (sb == null) {
            sb = new StringBuilder("{\"id\":").append(batchIncrement).append(",\"entries\":[").append(logString);
        } else {
            sb.append(",").append(logString);
        }
        currentBatchSize++;
        if (currentBatchSize == flushBatchSize) {
            log.debug("Cancel time-based flush and trigger buffer size flush.");
            // Flush because of batch size
            if (nextFlush != null) {
                nextFlush.cancel(false);
            }
            flush();
        }
    }

    private synchronized void flush() {
        try {
            nextFlush = null;
            if (currentBatchSize == 0) {
                log.debug("No data to flush.");
                scheduleFlush();
                return;
            }
            log.debug("Flushing ");
            sb.append("]}");
            Files.write(registrationPath.resolve(batchIncrement + ".json"), sb.toString().getBytes());

            if (batchToDeliver == -1) {
                batchToDeliver = batchIncrement;
            }

            batchIncrement++;
            currentBatchSize = 0;
            sb = null;

            scheduleFlush();
        } catch (IOException e) {
            log.error("Unable to flush log batch - logs will be lost.", e);
        }
    }

    private void scheduleFlush() {
        log.debug("Scheduling next time-based flush.");
        nextFlush = executor.schedule(() -> flush(), flushTimeoutSeconds, TimeUnit.SECONDS);
    }

    public String getLogs() throws IOException {
        // Get the next batch of logs.
        if (batchToDeliver == -1) {
            if (currentBatchSize == 0) {
                return "{}";
            }
            // We flush logs on disk before sending them.
            flush();
        }

        // Read the file content as a string
        return new String(Files.readAllBytes(registrationPath.resolve(batchToDeliver + ".json")));
    }

    public synchronized void ackLogs(long batchId) {
        if (batchId != batchToDeliver) {
            log.warn("Received ack request for batch <" + batchId + "> while was expecting <" + batchToDeliver + ">.");
        }
        File ackBatchFile = registrationPath.resolve(batchId + ".json").toFile();
        if (ackBatchFile.exists()) {
            ackBatchFile.delete();
        }
        batchToDeliver++;
    }

    public synchronized void remove() throws IOException {
        // Close the queue and remove all data
        active = false;
        if (nextFlush != null) {
            nextFlush.cancel(true);
        }
        if (registrationPath.toFile().exists()) {
            FileUtil.delete(registrationPath);
        }
    }
}
