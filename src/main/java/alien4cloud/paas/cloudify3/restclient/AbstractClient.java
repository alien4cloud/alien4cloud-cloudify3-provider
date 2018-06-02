package alien4cloud.paas.cloudify3.restclient;

import alien4cloud.paas.cloudify3.restclient.auth.AuthenticationInterceptor;
import com.google.common.collect.Maps;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Map.Entry;

/**
 * All clients are state less they must
 */
@Slf4j
@Setter
public abstract class AbstractClient {

    @Resource(name = "cloudify-async-rest-template")
    public void setRestTemplate(AsyncRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private AsyncRestTemplate restTemplate;

    @Resource
    private AuthenticationInterceptor authenticationInterceptor;

    /**
     * Get the url appended with the given suffix
     *
     * @param suffix path of the action
     * @param parameterNames all parameters' name
     * @return the url suffixed
     */
    public String getSuffixedUrl(String managerUrl, String suffix, String... parameterNames) {
        String urlPrefix = managerUrl + getPath() + (suffix != null ? suffix : "");
        if (parameterNames != null && parameterNames.length > 0) {
            StringBuilder urlBuilder = new StringBuilder(urlPrefix);
            urlBuilder.append("?");
            for (String parameterName : parameterNames) {
                urlBuilder.append(parameterName).append("={").append(parameterName).append("}&");
            }
            urlBuilder.setLength(urlBuilder.length() - 1);
            return urlBuilder.toString();
        } else {
            return urlPrefix;
        }
    }

    protected HttpEntity<?> createHttpEntity(String managerUrl) {
        return createHttpEntity(managerUrl, new HttpHeaders());
    }

    protected HttpEntity<?> createHttpEntity(String managerUrl, HttpHeaders httpHeaders) {
        return authenticationInterceptor.addAuthenticationHeader(new HttpEntity(httpHeaders));
    }

    protected <T> HttpEntity<T> createHttpEntity(T body, HttpHeaders httpHeaders) {
        return authenticationInterceptor.addAuthenticationHeader(new HttpEntity<>(body, httpHeaders));
    }

    protected <T> ListenableFuture<ResponseEntity<T>> getForEntity(String managerUrl, Class<T> responseType, Object... uriVariables) {
        return restTemplate.exchange(managerUrl, HttpMethod.GET, createHttpEntity(managerUrl), responseType, uriVariables);
    }

    protected ListenableFuture<?> delete(String managerUrl, Object... urlVariables) {
        return restTemplate.exchange(managerUrl, HttpMethod.DELETE, createHttpEntity(managerUrl), (Class<?>) null, urlVariables);
    }

    protected <T> ListenableFuture<ResponseEntity<T>> exchange(String managerUrl, HttpMethod method, HttpEntity<?> requestEntity, Class<T> responseType,
            Object... uriVariables) {
        return restTemplate.exchange(managerUrl, method, authenticationInterceptor.addAuthenticationHeader(requestEntity), responseType, uriVariables);
    }

    /**
     * Get the base url
     *
     * @param parameterNames all parameters' name
     * @return the url
     */
    public String getBaseUrl(String managerUrl, String... parameterNames) {
        return getSuffixedUrl(managerUrl, null, parameterNames);
    }

    protected abstract String getPath();
}
