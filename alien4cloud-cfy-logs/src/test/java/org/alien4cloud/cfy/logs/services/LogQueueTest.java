package org.alien4cloud.cfy.logs.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import alien4cloud.utils.FileUtil;

/**
 * Test the log queue behaviour.
 */
public class LogQueueTest {
    private static final String LOG_DIRECTORY = "target/test-data/logQueueTest/testRegId";

    // Flush should be done immediately when batch size is reached
    @Test
    public void testFlushOnBatchSize() throws IOException {
        Path logDirectory = Paths.get(LOG_DIRECTORY);

        // Delete if exist so the test start on clean data.
        if (logDirectory.toFile().exists()) {
            FileUtil.delete(logDirectory);
        }

        LogQueue logQueue = new LogQueue("testRegId", logDirectory, 10, 100);
        for (int i = 0; i < 100; i++) {
            // a typic cfy event message
            logQueue.addLog(
                    "{\"event_type\":\"workflow_started\",\"timestamp\":\"2017-09-14T14:06:46.521Z\",\"message_code\":null,\"context\":{\"deployment_id\":\"simple_compute-Environment\",\"workflow_id\":\"a4c_uninstall\",\"execution_id\":\"2444dd60-59c4-40dc-90fc-0a9fe62eed52\",\"blueprint_id\":\"simple_compute-Environment\",\"operation\":\"\",\"node_id\":\"\",\"task_error_causes\":\"\"},\"message\":{\"text\":\"Starting 'a4c_uninstall' workflow execution\",\"arguments\":null},\"type\":\"cloudify_event\",\"@version\":\"1\",\"@timestamp\":\"2017-09-14T14:06:46.521Z\",\"tags\":[\"event\"]}");
            // a typic cfy log message
            logQueue.addLog(
                    "{\"level\":\"debug\",\"timestamp\":\"2017-09-14T14:06:50.182Z\",\"message_code\":null,\"context\":{\"blueprint_id\":\"simple_compute-Environment\",\"task_target\":\"cloudify.management\",\"workflow_id\":\"a4c_uninstall\",\"node_id\":\"Compute_onj8qq\",\"operation\":\"cloudify.interfaces.cloudify_agent.stop\",\"execution_id\":\"2444dd60-59c4-40dc-90fc-0a9fe62eed52\",\"task_id\":\"7e3ae2b3-b782-40d9-8af0-27c1a5d3f7c0\",\"plugin\":\"agent\",\"node_name\":\"Compute\",\"task_name\":\"cloudify_agent.installer.operations.stop\",\"task_queue\":\"cloudify.management\",\"deployment_id\":\"simple_compute-Environment\",\"task_error_causes\":\"\"},\"logger\":\"7e3ae2b3-b782-40d9-8af0-27c1a5d3f7c0\",\"type\":\"cloudify_log\",\"message\":{\"text\":\"Applying function:setter on Attribute <protocol>\"},\"@version\":\"1\",\"@timestamp\":\"2017-09-14T14:06:50.182Z\",\"tags\":[\"log\"]}");
        }
        Assert.assertEquals(2, Files.list(logDirectory).count());

        logQueue.remove();
    }

    // Flush should be done after timeout if batch size is not reached
    @Test
    public void testFlushOnBatchTime() throws IOException, InterruptedException {
        Path logDirectory = Paths.get(LOG_DIRECTORY);

        // Delete if exist so the test start on clean data.
        if (logDirectory.toFile().exists()) {
            FileUtil.delete(logDirectory);
        }

        LogQueue logQueue = new LogQueue("testRegId", logDirectory, 5, 100);
        for (int i = 0; i < 2; i++) {
            // a typic cfy event message
            logQueue.addLog(
                    "{\"event_type\":\"workflow_started\",\"timestamp\":\"2017-09-14T14:06:46.521Z\",\"message_code\":null,\"context\":{\"deployment_id\":\"simple_compute-Environment\",\"workflow_id\":\"a4c_uninstall\",\"execution_id\":\"2444dd60-59c4-40dc-90fc-0a9fe62eed52\",\"blueprint_id\":\"simple_compute-Environment\",\"operation\":\"\",\"node_id\":\"\",\"task_error_causes\":\"\"},\"message\":{\"text\":\"Starting 'a4c_uninstall' workflow execution\",\"arguments\":null},\"type\":\"cloudify_event\",\"@version\":\"1\",\"@timestamp\":\"2017-09-14T14:06:46.521Z\",\"tags\":[\"event\"]}");
            // a typic cfy log message
            logQueue.addLog(
                    "{\"level\":\"debug\",\"timestamp\":\"2017-09-14T14:06:50.182Z\",\"message_code\":null,\"context\":{\"blueprint_id\":\"simple_compute-Environment\",\"task_target\":\"cloudify.management\",\"workflow_id\":\"a4c_uninstall\",\"node_id\":\"Compute_onj8qq\",\"operation\":\"cloudify.interfaces.cloudify_agent.stop\",\"execution_id\":\"2444dd60-59c4-40dc-90fc-0a9fe62eed52\",\"task_id\":\"7e3ae2b3-b782-40d9-8af0-27c1a5d3f7c0\",\"plugin\":\"agent\",\"node_name\":\"Compute\",\"task_name\":\"cloudify_agent.installer.operations.stop\",\"task_queue\":\"cloudify.management\",\"deployment_id\":\"simple_compute-Environment\",\"task_error_causes\":\"\"},\"logger\":\"7e3ae2b3-b782-40d9-8af0-27c1a5d3f7c0\",\"type\":\"cloudify_log\",\"message\":{\"text\":\"Applying function:setter on Attribute <protocol>\"},\"@version\":\"1\",\"@timestamp\":\"2017-09-14T14:06:50.182Z\",\"tags\":[\"log\"]}");
        }
        Thread.sleep(10000);
        Assert.assertEquals(1, Files.list(logDirectory).count());

        logQueue.remove();
    }

    // If no logs data no file should be flushed
    @Test
    public void noFlushWhenNoLogs() throws IOException, InterruptedException {
        Path logDirectory = Paths.get(LOG_DIRECTORY);

        // Delete if exist so the test start on clean data.
        if (logDirectory.toFile().exists()) {
            FileUtil.delete(logDirectory);
        }

        LogQueue logQueue = new LogQueue("testRegId", logDirectory, 5, 100);
        Thread.sleep(10000);
        Assert.assertEquals(0, Files.list(logDirectory).count());

        logQueue.remove();
    }

    // Get logs when none should return empty result
    @Test
    public void getNothingShouldReturnEmptyResult() throws IOException {
        Path logDirectory = Paths.get(LOG_DIRECTORY);

        // Delete if exist so the test start on clean data.
        if (logDirectory.toFile().exists()) {
            FileUtil.delete(logDirectory);
        }

        LogQueue logQueue = new LogQueue("testRegId", logDirectory, 10, 100);
        Assert.assertEquals("{}", logQueue.getLogs());

        logQueue.remove();
    }

    // Get logs twice without ack should return same result
    @Test
    public void getLogTwiceShouldReturnSameResult() throws IOException, InterruptedException {
        Path logDirectory = Paths.get(LOG_DIRECTORY);

        // Delete if exist so the test start on clean data.
        if (logDirectory.toFile().exists()) {
            FileUtil.delete(logDirectory);
        }

        LogQueue logQueue = new LogQueue("testRegId", logDirectory, 5, 100);
        for (int i = 0; i < 2; i++) {
            // a typic cfy event message
            logQueue.addLog(
                    "{\"event_type\":\"workflow_started\",\"timestamp\":\"2017-09-14T14:06:46.521Z\",\"message_code\":null,\"context\":{\"deployment_id\":\"simple_compute-Environment\",\"workflow_id\":\"a4c_uninstall\",\"execution_id\":\"2444dd60-59c4-40dc-90fc-0a9fe62eed52\",\"blueprint_id\":\"simple_compute-Environment\",\"operation\":\"\",\"node_id\":\"\",\"task_error_causes\":\"\"},\"message\":{\"text\":\"Starting 'a4c_uninstall' workflow execution\",\"arguments\":null},\"type\":\"cloudify_event\",\"@version\":\"1\",\"@timestamp\":\"2017-09-14T14:06:46.521Z\",\"tags\":[\"event\"]}");
            // a typic cfy log message
            logQueue.addLog(
                    "{\"level\":\"debug\",\"timestamp\":\"2017-09-14T14:06:50.182Z\",\"message_code\":null,\"context\":{\"blueprint_id\":\"simple_compute-Environment\",\"task_target\":\"cloudify.management\",\"workflow_id\":\"a4c_uninstall\",\"node_id\":\"Compute_onj8qq\",\"operation\":\"cloudify.interfaces.cloudify_agent.stop\",\"execution_id\":\"2444dd60-59c4-40dc-90fc-0a9fe62eed52\",\"task_id\":\"7e3ae2b3-b782-40d9-8af0-27c1a5d3f7c0\",\"plugin\":\"agent\",\"node_name\":\"Compute\",\"task_name\":\"cloudify_agent.installer.operations.stop\",\"task_queue\":\"cloudify.management\",\"deployment_id\":\"simple_compute-Environment\",\"task_error_causes\":\"\"},\"logger\":\"7e3ae2b3-b782-40d9-8af0-27c1a5d3f7c0\",\"type\":\"cloudify_log\",\"message\":{\"text\":\"Applying function:setter on Attribute <protocol>\"},\"@version\":\"1\",\"@timestamp\":\"2017-09-14T14:06:50.182Z\",\"tags\":[\"log\"]}");
        }
        Thread.sleep(5000);
        Assert.assertEquals(1, Files.list(logDirectory).count());

        String firstBatch = logQueue.getLogs();

        for (int i = 0; i < 60; i++) {
            // a typic cfy event message
            logQueue.addLog(
                    "{\"event_type\":\"workflow_started\",\"timestamp\":\"2017-09-14T14:06:46.521Z\",\"message_code\":null,\"context\":{\"deployment_id\":\"simple_compute-Environment\",\"workflow_id\":\"a4c_uninstall\",\"execution_id\":\"2444dd60-59c4-40dc-90fc-0a9fe62eed52\",\"blueprint_id\":\"simple_compute-Environment\",\"operation\":\"\",\"node_id\":\"\",\"task_error_causes\":\"\"},\"message\":{\"text\":\"Starting 'a4c_uninstall' workflow execution\",\"arguments\":null},\"type\":\"cloudify_event\",\"@version\":\"1\",\"@timestamp\":\"2017-09-14T14:06:46.521Z\",\"tags\":[\"event\"]}");
            // a typic cfy log message
            logQueue.addLog(
                    "{\"level\":\"debug\",\"timestamp\":\"2017-09-14T14:06:50.182Z\",\"message_code\":null,\"context\":{\"blueprint_id\":\"simple_compute-Environment\",\"task_target\":\"cloudify.management\",\"workflow_id\":\"a4c_uninstall\",\"node_id\":\"Compute_onj8qq\",\"operation\":\"cloudify.interfaces.cloudify_agent.stop\",\"execution_id\":\"2444dd60-59c4-40dc-90fc-0a9fe62eed52\",\"task_id\":\"7e3ae2b3-b782-40d9-8af0-27c1a5d3f7c0\",\"plugin\":\"agent\",\"node_name\":\"Compute\",\"task_name\":\"cloudify_agent.installer.operations.stop\",\"task_queue\":\"cloudify.management\",\"deployment_id\":\"simple_compute-Environment\",\"task_error_causes\":\"\"},\"logger\":\"7e3ae2b3-b782-40d9-8af0-27c1a5d3f7c0\",\"type\":\"cloudify_log\",\"message\":{\"text\":\"Applying function:setter on Attribute <protocol>\"},\"@version\":\"1\",\"@timestamp\":\"2017-09-14T14:06:50.182Z\",\"tags\":[\"log\"]}");
        }
        Assert.assertEquals(2, Files.list(logDirectory).count());

        Assert.assertEquals(firstBatch, logQueue.getLogs());

        // Assert that there is 4 entries in the first batch
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> data = mapper.readValue(firstBatch, new TypeReference<Map<String, Object>>() {
        });
        Assert.assertEquals(0, data.get("id"));
        List<Object> entries = (List<Object>) data.get("entries");
        Assert.assertEquals(4, entries.size());

        // Trigger ack of the first batch
        logQueue.ackLogs(0);

        // Ensure we get a different batch
        String secondBatch = logQueue.getLogs();
        Assert.assertNotEquals(firstBatch, secondBatch);
        // Ensure we have 100 elements in second batch
        data = mapper.readValue(secondBatch, new TypeReference<Map<String, Object>>() {
        });
        Assert.assertEquals(1, data.get("id"));
        entries = (List<Object>) data.get("entries");
        Assert.assertEquals(100, entries.size());

        logQueue.remove();
    }

    @Test
    public void testLoadExistingData() throws IOException {
        Path logDirectory = Paths.get(LOG_DIRECTORY);

        // Delete if exist so the test start on clean data.
        if (logDirectory.toFile().exists()) {
            FileUtil.delete(logDirectory);
        }

        FileUtil.copy(Paths.get("src/test/resources/data/logqueue"), logDirectory);

        LogQueue logQueue = new LogQueue("testRegId", logDirectory, 5, 100);
        String firstBatch = logQueue.getLogs();

        // Assert that there is 4 entries in the first batch
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> data = mapper.readValue(firstBatch, new TypeReference<Map<String, Object>>() {
        });
        Assert.assertEquals(6, data.get("id"));
        List<Object> entries = (List<Object>) data.get("entries");
        Assert.assertEquals(4, entries.size());

        // Trigger ack of the first batch
        logQueue.ackLogs(6);

        // Ensure we get a different batch
        String secondBatch = logQueue.getLogs();
        Assert.assertNotEquals(firstBatch, secondBatch);
        // Ensure we have 100 elements in second batch
        data = mapper.readValue(secondBatch, new TypeReference<Map<String, Object>>() {
        });
        Assert.assertEquals(7, data.get("id"));
        entries = (List<Object>) data.get("entries");
        Assert.assertEquals(100, entries.size());

        logQueue.remove();
    }
}