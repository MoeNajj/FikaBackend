package com.bikeparking.backend.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.stream.Collectors;

@Component
@Order(1)
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
    private static final int MAX_PAYLOAD_LENGTH = 1000;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            logRequest(wrappedRequest);
            logResponse(wrappedResponse, duration);

            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String requestBody = getContentAsString(request.getContentAsByteArray(), request.getCharacterEncoding());
        
        logger.info("\n--> REQUEST START: {} {} from {}\n" +
                    "    Headers: {}\n" +
                    "    Body: {}",
            request.getMethod(),
            request.getRequestURI(),
            request.getRemoteAddr(),
            getRequestHeaders(request),
            requestBody.isEmpty() ? "No body" : requestBody.trim()
        );
    }

    private void logResponse(ContentCachingResponseWrapper response, long duration) {
        String responseBody = getContentAsString(response.getContentAsByteArray(), response.getCharacterEncoding());

        logger.info("\n<-- REQUEST END: {} | Status: {} | Duration: {}ms\n" +
                    "    Response Body: {}",
            response.getStatus(),
            duration,
            responseBody.isEmpty() ? "No body" : responseBody.trim()
        );
    }

    private String getContentAsString(byte[] buf, String encoding) {
        if (buf.length == 0) return "";
        try {
            return new String(buf, 0, Math.min(buf.length, MAX_PAYLOAD_LENGTH), encoding);
        } catch (UnsupportedEncodingException ex) {
            return "[unknown]";
        }
    }
    
    private String getRequestHeaders(ContentCachingRequestWrapper request) {
        return Collections.list(request.getHeaderNames()).stream()
                .filter(headerName -> headerName.equalsIgnoreCase("Content-Type") || headerName.equalsIgnoreCase("Authorization") || headerName.equalsIgnoreCase("User-Agent"))
                .map(headerName -> headerName + "=[" + request.getHeader(headerName) + "]")
                .collect(Collectors.joining(", "));
    }
}
