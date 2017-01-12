package alien4cloud.paas.cloudify3.restclient;

import alien4cloud.paas.cloudify3.model.Token;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.bind.DatatypeConverter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public class TestTokenClient extends AbstractRestClientTest{

    private static TokenClient tokenClient ;

    @BeforeClass
    public static void before() {
        initializeContext();
        tokenClient = configureClient(new TokenClient());
    }

    @Test
    public void testGetToken() {
        Token token = tokenClient.get();
        Assert.assertNotNull(token);
    }
}
