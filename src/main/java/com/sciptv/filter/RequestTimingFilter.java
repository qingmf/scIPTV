package com.sciptv.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class RequestTimingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestTimingFilter.class);
    private static final String START_NANOS = "sciptv.requestStartNanos";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        requestContext.setProperty(START_NANOS, System.nanoTime());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Long startNanos = (Long) requestContext.getProperty(START_NANOS);
        long durationMs = startNanos == null ? -1L : (System.nanoTime() - startNanos) / 1_000_000;
        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();
        int status = responseContext.getStatus();

        if (status >= 500) {
            log.error("Request failed: method={}, path={}, status={}, durationMs={}", method, path, status, durationMs);
        } else {
            log.info("Request finished: method={}, path={}, status={}, durationMs={}", method, path, status, durationMs);
        }
    }
}
