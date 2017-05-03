package alien4cloud.paas.cloudify3.restclient;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import alien4cloud.paas.cloudify3.model.Token;
import alien4cloud.paas.cloudify3.shared.restclient.TokenClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestTokenClient extends AbstractRestClientTest {

    private static TokenClient tokenClient;

    @BeforeClass
    public static void before() {
        initializeContext();
        tokenClient = getApiClient().getTokenClient();
    }

    @Test
    public void testGetToken() {
        Token token = tokenClient.get();
        Assert.assertNotNull(token);
    }
}
