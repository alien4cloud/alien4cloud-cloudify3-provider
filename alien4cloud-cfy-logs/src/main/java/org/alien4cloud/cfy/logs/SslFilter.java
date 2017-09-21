package org.alien4cloud.cfy.logs;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.collect.Lists;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link Filter} that checks, when SSL mode mode is enabled, if the request is to be processed depending on the given protocol and port. If SSL mode is
 * enabled, the filter only allows a specific request on HTTP protocol, throwing an exception otherwise.
 *
 * This bean is created only if the property <b>server.ssl.enabled</b> is set to <b>true</b> in the config file
 */
@Slf4j
@Component
@ConditionalOnProperty("server.ssl.enabled")
public class SslFilter implements Filter {

    private List<AuthorizedPath> authorizedPaths;

    @Value("${server.ssl.enabled:false}")
    Boolean sslEnabled;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialize the list of authorizedPaths
        authorizedPaths = Lists.newArrayList(new AuthorizedPath("/api/v1/logs", RequestMethod.POST));
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // Check if we are in ssl mode
        final HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        if (sslEnabled && StringUtils.equalsAnyIgnoreCase(servletRequest.getScheme(), "http")) {
            if (isDeniedPath(httpRequest)) {
                log.debug("SSL mode is enabled and deny request for {}", httpRequest.getRequestURL());

                HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
                httpResponse.setStatus(HttpStatus.FORBIDDEN.value());
                return;
            }
        }

        // Proceed with the request.
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @SneakyThrows(URISyntaxException.class)
    private boolean isDeniedPath(HttpServletRequest httpRequest) {
        String uriString = StringUtils.stripEnd(httpRequest.getRequestURI(), "/");
        URI uri = new URI(uriString);
        return !authorizedPaths.stream().anyMatch(authorizedPath -> authorizedPath.isAuthorized(uri, httpRequest.getMethod()));
    }

    @Override
    public void destroy() {
    }

    private class AuthorizedPath {
        String path;
        RequestMethod[] methods;

        public AuthorizedPath(String path, RequestMethod... methods) {
            this.path = path;
            this.methods = methods;
        }

        public boolean isAuthorized(URI uri, String method) {
            return uri.getPath().equals(this.path) && ArrayUtils.contains(this.methods, RequestMethod.valueOf(method));
        }
    }

}