package com.bikeparking.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.NonNull;

import jakarta.annotation.PostConstruct;

@Configuration
public class ServerConfig implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ServerConfig.class);

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${server.ssl.key-store:}")
    private String keyStore;

    @Value("${server.ssl.key-store-password:}")
    private String keyStorePassword;

    private final ResourceLoader resourceLoader;

    public ServerConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void logServerConfiguration() {
        // #region agent log
        try {
            java.io.FileWriter fw = new java.io.FileWriter("/home/moe/Cursor_Fika_Backend/FikaBackend/.cursor/debug.log", true);
            fw.write("{\"sessionId\":\"debug-session\",\"runId\":\"startup\",\"hypothesisId\":\"A\",\"location\":\"ServerConfig.java:PostConstruct\",\"message\":\"Server configuration loaded\",\"data\":{\"serverPort\":" + serverPort + ",\"keyStore\":\"" + keyStore + "\",\"keyStorePasswordSet\":" + (!keyStorePassword.isEmpty()) + ",\"userName\":\"" + System.getProperty("user.name") + "\"},\"timestamp\":" + System.currentTimeMillis() + "}\n");
            fw.close();
        } catch (Exception e) {}
        // #endregion
        
        logger.info("===============================================");
        logger.info("Server Configuration:");
        logger.info("  Port: {}", serverPort);
        logger.info("  Key Store: {}", keyStore);
        logger.info("  Key Store Password Set: {}", !keyStorePassword.isEmpty());
        logger.info("  Current User: {}", System.getProperty("user.name"));
        
        // Check if certificate file exists
        if (keyStore != null && !keyStore.isEmpty()) {
            String certPath = keyStore;
            if (certPath.startsWith("classpath:")) {
                certPath = certPath.substring("classpath:".length());
                try {
                    Resource resource = resourceLoader.getResource("classpath:" + certPath);
                    boolean exists = resource.exists();
                    long size = exists ? resource.contentLength() : 0;
                    
                    // #region agent log
                    try {
                        java.io.FileWriter fw = new java.io.FileWriter("/home/moe/Cursor_Fika_Backend/FikaBackend/.cursor/debug.log", true);
                        fw.write("{\"sessionId\":\"debug-session\",\"runId\":\"startup\",\"hypothesisId\":\"B\",\"location\":\"ServerConfig.java:PostConstruct\",\"message\":\"Certificate file check (classpath)\",\"data\":{\"certPath\":\"" + certPath + "\",\"exists\":" + exists + ",\"size\":" + size + "},\"timestamp\":" + System.currentTimeMillis() + "}\n");
                        fw.close();
                    } catch (Exception e) {}
                    // #endregion
                    
                    logger.info("  Certificate File (classpath): {}", certPath);
                    logger.info("  Certificate File Exists: {}", exists);
                    if (exists) {
                        logger.info("  Certificate File Size: {} bytes", size);
                    }
                } catch (Exception e) {
                    logger.error("  Error checking certificate: {}", e.getMessage());
                }
            }
        }
        
        // Check if port requires privileges (ports < 1024 require root)
        if (serverPort < 1024 && serverPort != 443) {
            // #region agent log
            try {
                java.io.FileWriter fw = new java.io.FileWriter("/home/moe/Cursor_Fika_Backend/FikaBackend/.cursor/debug.log", true);
                fw.write("{\"sessionId\":\"debug-session\",\"runId\":\"startup\",\"hypothesisId\":\"C\",\"location\":\"ServerConfig.java:PostConstruct\",\"message\":\"Privileged port check\",\"data\":{\"port\":" + serverPort + ",\"isRoot\":" + (System.getProperty("user.name").equals("root")) + "},\"timestamp\":" + System.currentTimeMillis() + "}\n");
                fw.close();
            } catch (Exception e) {}
            // #endregion
            
            logger.warn("  ⚠️  Port {} requires root/admin privileges!", serverPort);
            logger.warn("  ⚠️  If not running as root, Spring Boot may fail to bind to port {}", serverPort);
        } else if (serverPort >= 1024) {
            // #region agent log
            try {
                java.io.FileWriter fw = new java.io.FileWriter("/home/moe/Cursor_Fika_Backend/FikaBackend/.cursor/debug.log", true);
                fw.write("{\"sessionId\":\"debug-session\",\"runId\":\"startup\",\"hypothesisId\":\"C\",\"location\":\"ServerConfig.java:PostConstruct\",\"message\":\"Non-privileged port\",\"data\":{\"port\":" + serverPort + ",\"isRoot\":" + (System.getProperty("user.name").equals("root")) + "},\"timestamp\":" + System.currentTimeMillis() + "}\n");
                fw.close();
            } catch (Exception e) {}
            // #endregion
            
            logger.info("  ✅ Port {} does not require root privileges", serverPort);
        }
        
        logger.info("===============================================");
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        int actualPort = event.getApplicationContext().getBean(org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.class)
                .getWebServer().getPort();
        
        // #region agent log
        try {
            java.io.FileWriter fw = new java.io.FileWriter("/home/moe/Cursor_Fika_Backend/FikaBackend/.cursor/debug.log", true);
            fw.write("{\"sessionId\":\"debug-session\",\"runId\":\"startup\",\"hypothesisId\":\"F\",\"location\":\"ServerConfig.java:onApplicationEvent\",\"message\":\"Application started - actual port\",\"data\":{\"configuredPort\":" + serverPort + ",\"actualPort\":" + actualPort + ",\"portsMatch\":" + (serverPort == actualPort) + "},\"timestamp\":" + System.currentTimeMillis() + "}\n");
            fw.close();
        } catch (Exception e) {}
        // #endregion
        
        logger.info("===============================================");
        logger.info("Application Started:");
        logger.info("  Configured Port: {}", serverPort);
        logger.info("  Actual Port: {}", actualPort);
        if (serverPort != actualPort) {
            logger.error("  ❌ PORT MISMATCH! Configured {} but running on {}", serverPort, actualPort);
            logger.error("  ❌ This likely means Spring Boot failed to bind to port {}", serverPort);
        } else {
            logger.info("  ✅ Port matches configuration");
        }
        logger.info("===============================================");
    }

    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        
        // #region agent log
        try {
            java.io.FileWriter fw = new java.io.FileWriter("/home/moe/Cursor_Fika_Backend/FikaBackend/.cursor/debug.log", true);
            fw.write("{\"sessionId\":\"debug-session\",\"runId\":\"startup\",\"hypothesisId\":\"D\",\"location\":\"ServerConfig.java:servletContainer\",\"message\":\"Creating servlet container\",\"data\":{\"configuredPort\":" + serverPort + "},\"timestamp\":" + System.currentTimeMillis() + "}\n");
            fw.close();
        } catch (Exception e) {}
        // #endregion
        
        factory.setPort(serverPort);
        
        factory.addConnectorCustomizers(connector -> {
            // #region agent log
            try {
                java.io.FileWriter fw = new java.io.FileWriter("/home/moe/Cursor_Fika_Backend/FikaBackend/.cursor/debug.log", true);
                fw.write("{\"sessionId\":\"debug-session\",\"runId\":\"startup\",\"hypothesisId\":\"E\",\"location\":\"ServerConfig.java:addConnectorCustomizers\",\"message\":\"Connector customized\",\"data\":{\"port\":" + connector.getPort() + ",\"scheme\":\"" + connector.getScheme() + "\",\"secure\":" + connector.getSecure() + "},\"timestamp\":" + System.currentTimeMillis() + "}\n");
                fw.close();
            } catch (Exception e) {}
            // #endregion
            
            logger.info("Connector configured: Port={}, Scheme={}, Secure={}", 
                connector.getPort(), connector.getScheme(), connector.getSecure());
        });
        
        return factory;
    }
}

