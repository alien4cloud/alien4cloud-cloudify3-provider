package alien4cloud.paas.cloudify3.loganalyser;

import alien4cloud.paas.cloudify3.loganalyser.model.DeployedTopology;
import alien4cloud.paas.cloudify3.loganalyser.model.OrchestratorLog;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Small piece of code that connects to an A4C elasticsearch via it's REST API and compare logs between 2 deployments.
 * <p/>
 * In order to compare logs, fist use {@link #listDeployments()} which will give you all deployments and the logs count.
 * Then use {@link #compareLogs()} using the right deploymentIds.
 * <p/>
 * The comparison is done like this:
 * <ul>
 * <li>All logs are anonymized (elements that change between deployments are replaced) in order to define a pattern.</li>
 * <li>For a deployment, group logs by patterns</li>
 * <li>Compare the patterns between deployments :</li>
 * <ul>
 * <li>if a pattern is found in a deployment and not in the other, then fail (don't match)</li>
 * <li>if occurences per pattern differ between 2 deployments, then fail</li>
 * </ul>
 * </ul>
 * Logs entries are considered as equivalent if they have the same patterns with the same occurrences.
 * <p/>
 * False negatives can occur if the anonymization is not correct, feel free to complete {@link LogUtils#anonymize(String)} (but take care to avoid false positives).
 */
@Slf4j
@RunWith(JUnit4.class)
public class PassDeploymentLogTest {

    private static final String A4C_ES_URL = "http://34.245.182.103:80";

    private static final String MATCH_ALL_QUERY = "{\"query\": { \"match_all\": {} },\"size\": 1000}";
    private static final String MATCH_STRING_QUERY = "{\"query\": { \"match\": { \"%s\": \"%s\" } },\"size\": 1000}";
    private static final String MATCH_SINGLE_QUERY = "{\"query\": { \"match\": { \"%s\": \"%s\" } },\"size\": 1}";

    private JestClient client;

    @Before
    public void init() {
        HttpClientConfig httpClientConfig = new HttpClientConfig.Builder(A4C_ES_URL).build();
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(httpClientConfig);
        client = factory.getObject();
    }

    @Test
    public void listDeployments() {
        init();
        SearchResult result = getAll("deployedtopologies");
        List<SearchResult.Hit<DeployedTopology, Void>> logs = result.getHits(DeployedTopology.class);
        logs.stream().forEach(l -> {
            int logCount = countMatch("paasdeploymentlog", "deploymentId", l.source.id);
            log.info("{} : {} has {} logs", l.source.id, l.source.archiveName, logCount);
        });
    }

    @Test
    public void compareLogs() {
        init();
        String leftDeploymentId = "586b7ed5-8a62-4627-8e2f-187f5a04fb39";
        String rightDeploymentId = "1d21f524-9045-42a2-92ad-be115d3073ca";

        DeploymentLogs leftLogs = getLogPatterns(leftDeploymentId);
        DeploymentLogs rightLogs = getLogPatterns(rightDeploymentId);
        boolean match = compareTwoLogFileByMap(leftLogs, rightLogs);
        log.info("{} & {} {} match !", leftLogs.deployment.archiveName, rightLogs.deployment.archiveName, match ? "did" : "did NOT");
    }

    public boolean compareTwoLogFileByMap(DeploymentLogs left, DeploymentLogs right) {

        boolean result = true;
        for (Map.Entry<String, List<OrchestratorLog>> leftEntry : left.logsByPattern.entrySet()) {
            String pattern = leftEntry.getKey();
            List<OrchestratorLog> leftList = leftEntry.getValue();
            String abreviation = StringUtils.abbreviate(pattern, 50);
            if (right.logsByPattern.containsKey(leftEntry.getKey())) {
                // entry exists
                List<OrchestratorLog> rightList = right.logsByPattern.get(pattern);
                if (leftList.size() != rightList.size()) {
                    result = false;
                    // the 2 lists have different size
                    // TODO: log
                    log.info("<{}> found in both but different sizes: {} != {}", abreviation, leftList.size(), rightList.size());
                    log.info("{} has:", left.deployment.archiveName);
                    List<OrchestratorLog> onlyInLeft = ListUtils.removeAll(leftList, rightList);
                    List<OrchestratorLog> onlyInRight = ListUtils.removeAll(rightList, leftList);
                    onlyInLeft.stream().forEach(orchestratorLog -> {
                        logLog(orchestratorLog);
                    });
                    log.info("{} has:", right.deployment.archiveName);
                    onlyInRight.stream().forEach(orchestratorLog -> {
                        logLog(orchestratorLog);
                    });
                } else {
//                    log.info("<{}> found in both with same sizes: {}", abreviation, leftList.size());
                }
            } else {
                result = false;
                // this log pattern exist in left but not in right
                log.info("<{}> pattern found in left ({} entries) but not in right: {}", abreviation, leftList.size(), pattern);
                leftList.stream().forEach(orchestratorLog -> {
                    logLog(orchestratorLog);
                });
            }
        }
        // now just try to find elements that are in right but not in left
        for (Map.Entry<String, List<OrchestratorLog>> rightEntry : right.logsByPattern.entrySet()) {
            if (!left.logsByPattern.containsKey(rightEntry.getKey())) {
                result = false;
                String pattern = rightEntry.getKey();
                String abreviation = StringUtils.abbreviate(pattern, 50);
                // this log pattern exist in right but not in left
                log.info("<{}> pattern found in right ({} entries) but not in left: {}", abreviation, rightEntry.getValue().size(), pattern);
                rightEntry.getValue().stream().forEach(orchestratorLog -> {
                    logLog(orchestratorLog);
                });
            }
        }
        return result;
    }

    private void logLog(OrchestratorLog l) {
        log.info("\t{} - {}", l.timestamp, l.content);
        log.info("\t{} -  == {}", l.timestamp, l.pattern);
    }

    private DeploymentLogs getLogPatterns(String deploymentId) {

        // get the deployment
        SearchResult result = getSingle("deployedtopologies", "id", deploymentId);
        result.getHits(DeployedTopology.class).get(0);
        DeploymentLogs deploymentLogs = new DeploymentLogs();
        deploymentLogs.deployment = result.getHits(DeployedTopology.class).get(0).source;
        deploymentLogs.logsByPattern = Maps.newHashMap();

        result = getMatch("paasdeploymentlog", "deploymentId", deploymentId);
        List<SearchResult.Hit<OrchestratorLog, Void>> logs = result.getHits(OrchestratorLog.class);
        log.debug("Found {} logs for deployment {}, {} returned", result.getTotal(), deploymentLogs.deployment.archiveName, logs.size());
        logs.stream().forEach(l -> {
            String pattern = LogUtils.anonymize(l.source.getContent());
            List<OrchestratorLog> logPatterns = deploymentLogs.logsByPattern.get(pattern);
            if (logPatterns == null) {
                logPatterns = Lists.newArrayList();
                deploymentLogs.logsByPattern.put(pattern, logPatterns);
            }
            l.source.setPattern(pattern);
            logPatterns.add(l.source);
            deploymentLogs.logCount++;
        });
        deploymentLogs.logsByPattern.forEach((pattern, orchestratorLogs) -> {
            log.info("For {} found pattern <{}> with {} entries", deploymentLogs.deployment.archiveName, StringUtils.abbreviate(pattern, 50), orchestratorLogs.size());
        });
        return deploymentLogs;
    }

    private SearchResult getAll(String index) {
        Search.Builder searchDeployedTopologiesBuilder = new Search.Builder(MATCH_ALL_QUERY).addIndex(index);
        SearchResult result = null;
        try {
            result = client.execute(searchDeployedTopologiesBuilder.build());
        } catch (IOException e) {
            log.error("Not able get all", e);
        }
        return result;
    }

    @SneakyThrows
    private SearchResult getMatch(String index, String paramName, String paramValue) {
        String query = String.format(MATCH_STRING_QUERY, paramName, paramValue);
        Search.Builder searchBuilder = new Search.Builder(query).addIndex(index);
        SearchResult result = null;
        return client.execute(searchBuilder.build());
    }

    @SneakyThrows
    private SearchResult getSingle(String index, String paramName, String paramValue) {
        String query = String.format(MATCH_SINGLE_QUERY, paramName, paramValue);
        Search.Builder searchBuilder = new Search.Builder(query).addIndex(index);
        SearchResult result = null;
        result = client.execute(searchBuilder.build());
        if (result.getTotal() != 1) {
            throw new RuntimeException("Not a single result");
        }
        return result;
    }

    @SneakyThrows
    private int countMatch(String index, String paramName, String paramValue) {
        String query = String.format(MATCH_SINGLE_QUERY, paramName, paramValue);
        Search.Builder searchBuilder = new Search.Builder(query).addIndex(index);
        SearchResult execute = null;
        execute = client.execute(searchBuilder.build());
        return execute.getTotal();
    }

    private class DeploymentLogs {
        public DeployedTopology deployment;
        public int logCount;
        public Map<String, List<OrchestratorLog>> logsByPattern;
    }

}
