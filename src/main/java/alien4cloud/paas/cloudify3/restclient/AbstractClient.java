package alien4cloud.paas.cloudify3.restclient;

import alien4cloud.paas.cloudify3.restclient.auth.AuthenticationInterceptor;
import com.google.common.collect.Maps;
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

@Slf4j
public abstract class AbstractClient {

    @Resource(name = "cloudify-async-rest-template")
    public void setRestTemplate(AsyncRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private AsyncRestTemplate restTemplate;

    @Resource
    private AuthenticationInterceptor authenticationInterceptor;

    private Map<String, AuthenticationInterceptor> authenticationInterceptorByManager = Maps.newHashMap();

    /** Allows registration of authentication interceptors by manager for the multiplexer. */
    public void registerAuthenticationManager(String managerUrl, AuthenticationInterceptor authenticationInterceptor) {
        if (authenticationInterceptorByManager.containsKey(managerUrl)) {
            log.info("Overriding authentication interceptor for manager {}", managerUrl);
        } else {
            log.info("Register an authentication interceptor for manager {}", managerUrl);
        }
        authenticationInterceptorByManager.put(managerUrl, authenticationInterceptor);
    }

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
        AuthenticationInterceptor interceptor = getInterceptorForUrl(managerUrl);
        if (interceptor == null) {
            interceptor = authenticationInterceptor;
        }
        return interceptor.addAuthenticationHeader(new HttpEntity(httpHeaders));
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
        AuthenticationInterceptor interceptor = getInterceptorForUrl(managerUrl);
        if (interceptor == null) {
            interceptor = authenticationInterceptor;
        }
        return restTemplate.exchange(managerUrl, method, interceptor.addAuthenticationHeader(requestEntity), responseType, uriVariables);
    }

    private AuthenticationInterceptor getInterceptorForUrl(String queryUrl) {
        for (Entry<String, AuthenticationInterceptor> interceptorEntry : authenticationInterceptorByManager.entrySet()) {
            if (queryUrl.startsWith(interceptorEntry.getKey())) {
                return interceptorEntry.getValue();
            }
        }
        return null;
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
