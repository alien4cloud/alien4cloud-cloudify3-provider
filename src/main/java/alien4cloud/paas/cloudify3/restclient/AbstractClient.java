package alien4cloud.paas.cloudify3.restclient;

import javax.annotation.Resource;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import alien4cloud.paas.cloudify3.configuration.CloudConfigurationHolder;
import alien4cloud.paas.cloudify3.restclient.auth.AuthenticationInterceptor;
import lombok.Getter;
import lombok.Setter;

public abstract class AbstractClient {

    @Resource(name = "cloudify-async-rest-template")
    @Setter
    private AsyncRestTemplate restTemplate;

    @Resource
    @Getter
    @Setter
    private CloudConfigurationHolder configurationHolder;

    @Resource
    @Setter
    private AuthenticationInterceptor authenticationInterceptor;

    /**
     * Get the url appended with the given suffix
     *
     * @param suffix path of the action
     * @param parameterNames all parameters' name
     * @return the url suffixed
     */
    public String getSuffixedUrl(String suffix, String... parameterNames) {
        String urlPrefix = configurationHolder.getConfiguration().getUrl() + "/api/" + configurationHolder.getConfiguration().getApiVersion() + getPath()
                + (suffix != null ? suffix : "");
        if (parameterNames != null && parameterNames.length > 0) {
            UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(urlPrefix);
            for (String parameterName : parameterNames) {
                uriComponentsBuilder.queryParam(parameterName, "{" + parameterName + "}");
            }
            return uriComponentsBuilder.build().toUriString();
        } else {
            return urlPrefix;
        }
    }

    protected HttpEntity<?> createHttpEntity() {
        return createHttpEntity(new HttpHeaders());
    }

    protected HttpEntity<?> createHttpEntity(HttpHeaders httpHeaders) {
        return authenticationInterceptor.addAuthenticationHeader(new HttpEntity(httpHeaders));
    }

    protected <T> HttpEntity<T> createHttpEntity(T body, HttpHeaders httpHeaders) {
        return authenticationInterceptor.addAuthenticationHeader(new HttpEntity<>(body, httpHeaders));
    }

    protected <T> ListenableFuture<ResponseEntity<T>> getForEntity(String url, Class<T> responseType, Object... uriVariables) {
        return restTemplate.exchange(url, HttpMethod.GET, createHttpEntity(), responseType, uriVariables);
    }

    protected ListenableFuture<?> delete(String url, Object... urlVariables) {
        return restTemplate.exchange(url, HttpMethod.DELETE, createHttpEntity(), (Class<?>) null, urlVariables);
    }

    protected <T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity, Class<T> responseType,
            Object... uriVariables) {
        return restTemplate.exchange(url, method, authenticationInterceptor.addAuthenticationHeader(requestEntity), responseType, uriVariables);
    }

    /**
     * Get the base url
     *
     * @param parameterNames all parameters' name
     * @return the url
     */
    public String getBaseUrl(String... parameterNames) {
        return getSuffixedUrl(null, parameterNames);
    }

    protected abstract String getPath();
}
