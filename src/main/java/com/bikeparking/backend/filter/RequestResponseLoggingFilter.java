package com.bikeparking.backend.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.stream.Collectors;

@Component
@Order(1)
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
    private static final int MAX_PAYLOAD_LENGTH = 5000; // Increased for better logging
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Skip logging for actuator endpoints and static resources
        String requestURI = request.getRequestURI();
        if (shouldSkipLogging(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            logRequest(wrappedRequest);
            logResponse(wrappedResponse, duration, wrappedRequest.getRequestURI());

            wrappedResponse.copyBodyToResponse();
        }
    }

    private boolean shouldSkipLogging(String uri) {
        return uri.startsWith("/actuator") || 
               uri.startsWith("/favicon.ico") || 
               uri.startsWith("/error");
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        try {
            String method = request.getMethod();
            String uri = request.getRequestURI();
            String queryString = request.getQueryString();
            String fullUrl = queryString != null ? uri + "?" + queryString : uri;
            String remoteAddr = request.getRemoteAddr();
            String remoteHost = request.getRemoteHost();
            
            // Get all headers
            String headers = getAllHeaders(request);
            
            // Get request body and mask sensitive data
            String requestBody = getContentAsString(
                request.getContentAsByteArray(), 
                request.getCharacterEncoding()
            );
            String sanitizedBody = sanitizeRequestBody(uri, requestBody);
            
            logger.info("\n" +
                "╔════════════════════════════════════════════════════════════════════════════════╗\n" +
                "║ REQUEST INCOMING                                                               ║\n" +
                "╠════════════════════════════════════════════════════════════════════════════════╣\n" +
                "║ Method    : {}\n" +
                "║ URL       : {}\n" +
                "║ Remote    : {} ({})\n" +
                "║ Headers   : {}\n" +
                "║ Body      : {}\n" +
                "╚════════════════════════════════════════════════════════════════════════════════╝",
                method,
                fullUrl,
                remoteAddr,
                remoteHost,
                headers.isEmpty() ? "None" : headers,
                sanitizedBody.isEmpty() ? "[No body]" : sanitizedBody
            );
        } catch (Exception e) {
            logger.warn("Error logging request: {}", e.getMessage());
        }
    }

    private void logResponse(ContentCachingResponseWrapper response, long duration, String requestUri) {
        try {
            int status = response.getStatus();
            String statusText = getStatusText(status);
            
            // Get response body
            String responseBody = getContentAsString(
                response.getContentAsByteArray(),
                response.getCharacterEncoding()
            );
            String sanitizedBody = sanitizeResponseBody(requestUri, responseBody);
            
            String statusColor = getStatusColor(status);
            
            logger.info("\n" +
                "╔════════════════════════════════════════════════════════════════════════════════╗\n" +
                "║ RESPONSE OUTGOING                                                             ║\n" +
                "╠════════════════════════════════════════════════════════════════════════════════╣\n" +
                "║ Status    : {} {} ({})\n" +
                "║ Duration  : {} ms\n" +
                "║ URI       : {}\n" +
                "║ Body      : {}\n" +
                "╚════════════════════════════════════════════════════════════════════════════════╝",
                status,
                statusText,
                statusColor,
                duration,
                requestUri,
                sanitizedBody.isEmpty() ? "[No body]" : sanitizedBody
            );
        } catch (Exception e) {
            logger.warn("Error logging response: {}", e.getMessage());
        }
    }

    private String getAllHeaders(ContentCachingRequestWrapper request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames == null || !headerNames.hasMoreElements()) {
            return "None";
        }
        
        return Collections.list(headerNames).stream()
                .map(headerName -> {
                    String headerValue = request.getHeader(headerName);
                    // Mask Authorization token for security
                    if (headerName.equalsIgnoreCase("Authorization") && headerValue != null && headerValue.startsWith("Bearer ")) {
                        String token = headerValue.substring(7);
                        if (token.length() > 20) {
                            headerValue = "Bearer " + token.substring(0, 10) + "..." + token.substring(token.length() - 10);
                        }
                    }
                    return headerName + "=[" + headerValue + "]";
                })
                .collect(Collectors.joining(", "));
    }

    private String sanitizeRequestBody(String uri, String body) {
        if (body == null || body.isEmpty()) {
            return "";
        }

        // Mask passwords in login/register requests
        if (uri.contains("/api/auth/login") || uri.contains("/api/auth/register")) {
            try {
                ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(body);
                if (jsonNode.has("password")) {
                    jsonNode.put("password", "***MASKED***");
                }
                return jsonNode.toString();
            } catch (Exception e) {
                // If not JSON or parsing fails, just mask the word "password"
                return body.replaceAll("(\"password\"\\s*:\\s*\")([^\"]+)(\")", "$1***MASKED***$3");
            }
        }

        // Truncate if too long
        if (body.length() > MAX_PAYLOAD_LENGTH) {
            return body.substring(0, MAX_PAYLOAD_LENGTH) + "... [truncated]";
        }

        return body;
    }

    private String sanitizeResponseBody(String uri, String body) {
        if (body == null || body.isEmpty()) {
            return "";
        }

        // Mask tokens in auth responses
        if (uri.contains("/api/auth/")) {
            try {
                ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(body);
                if (jsonNode.has("accessToken")) {
                    String token = jsonNode.get("accessToken").asText();
                    if (token.length() > 20) {
                        jsonNode.put("accessToken", token.substring(0, 10) + "..." + token.substring(token.length() - 10));
                    }
                }
                if (jsonNode.has("refreshToken")) {
                    String token = jsonNode.get("refreshToken").asText();
                    if (token.length() > 20) {
                        jsonNode.put("refreshToken", token.substring(0, 10) + "..." + token.substring(token.length() - 10));
                    }
                }
                return jsonNode.toString();
            } catch (Exception e) {
                // If not JSON, return as is but truncated
            }
        }

        // Truncate if too long
        if (body.length() > MAX_PAYLOAD_LENGTH) {
            return body.substring(0, MAX_PAYLOAD_LENGTH) + "... [truncated]";
        }

        return body;
    }

    private String getContentAsString(byte[] buf, String encoding) {
        if (buf == null || buf.length == 0) {
            return "";
        }
        try {
            String enc = encoding != null ? encoding : StandardCharsets.UTF_8.name();
            return new String(buf, 0, Math.min(buf.length, MAX_PAYLOAD_LENGTH), enc);
        } catch (UnsupportedEncodingException ex) {
            try {
                return new String(buf, 0, Math.min(buf.length, MAX_PAYLOAD_LENGTH), StandardCharsets.UTF_8);
            } catch (Exception e) {
                return "[Unable to decode]";
            }
        }
    }

    private String getStatusText(int status) {
        return switch (status) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            case 500 -> "Internal Server Error";
            default -> "Unknown";
        };
    }

    private String getStatusColor(int status) {
        if (status >= 200 && status < 300) return "✓ SUCCESS";
        if (status >= 400 && status < 500) return "⚠ CLIENT ERROR";
        if (status >= 500) return "✗ SERVER ERROR";
        return "? UNKNOWN";
    }
}
