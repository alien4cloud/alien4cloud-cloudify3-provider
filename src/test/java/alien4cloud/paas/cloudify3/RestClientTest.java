package alien4cloud.paas.cloudify3;

import org.springframework.web.client.RestTemplate;

/**
 * Created by victor on 22/04/2016.
 */
public class RestClientTest {
  public static void main(String[] args) {
    System.setProperty("javax.net.ssl.keyStoreType", "jks");
    System.setProperty("javax.net.ssl.keyStore", "C:\\tmp\\namedca\\client-keystore.jks");
    System.setProperty("javax.net.ssl.keyStorePassword", "azerty");
    System.setProperty("javax.net.ssl.trustStoreType", "jks");
    System.setProperty("javax.net.ssl.trustStore", "C:\\tmp\\namedca\\server-truststore.jks");
    System.setProperty("javax.net.ssl.trustStorePassword", "azerty");

    RestTemplate template = new RestTemplate();
    String response = template.getForEntity("https://129.185.67.114:8199/version",String.class).getBody();
    System.out.println(response);
  }
}
