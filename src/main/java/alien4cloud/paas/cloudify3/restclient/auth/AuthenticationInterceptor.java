package alien4cloud.paas.cloudify3.restclient.auth;

import lombok.Setter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Component
public class AuthenticationInterceptor {

    private static final String CLOUDIFY_TENANT_HEADER = "Tenant";
    private static final String CLOUDIFY_DEFAULT_TENANT = "default_tenant";

    private String userName;

    private String password;

    @Setter
    private String tenant = CLOUDIFY_DEFAULT_TENANT;

    private String encodedCredentials;

    public <T> HttpEntity<T> addAuthenticationHeader(HttpEntity<T> request) {
        HttpHeaders headers = new HttpHeaders();
        // Add Tenant since Cloudify 4.0m12
        headers.add(CLOUDIFY_TENANT_HEADER, this.tenant);
        headers.add(HttpHeaders.AUTHORIZATION, "Basic " + this.encodedCredentials);
        if (!request.getHeaders().isEmpty()) {
            headers.putAll(request.getHeaders());
        }
        return new HttpEntity<>(request.getBody(), headers);
    }

    private String encodeCredentials() {
        if (userName != null && password != null) {
            String plainCredentials = userName + ":" + password;
            return new String(Base64.encode(plainCredentials.getBytes()));
        } else {
            return null;
        }
    }

    public void setUserName(String userName) {
        this.userName = userName;
        this.encodedCredentials = encodeCredentials();
    }

    public void setPassword(String password) {
        this.password = password;
        this.encodedCredentials = encodeCredentials();
    }
}
