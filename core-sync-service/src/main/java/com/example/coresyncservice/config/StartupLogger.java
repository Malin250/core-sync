package com.example.coresyncservice.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Prints useful URLs to the log once the application has started.
 */
@Component
@RequiredArgsConstructor
public class StartupLogger implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupLogger.class);

    private final Environment environment;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${springdoc.swagger-ui.path:/swagger-ui.html}")
    private String swaggerUiPath;

    @Override
    public void run(String... args) {
        String ip = "localhost";
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            log.warn("Could not determine local IP address; falling back to 'localhost'.");
        }

        String base    = String.format("http://%s:%s%s", ip, serverPort, contextPath);
        String swagger = base + swaggerUiPath;

        log.info("----------------------------------------------------------");
        log.info("Application '{}' is running!",
                environment.getProperty("spring.application.name", "Core Sync Service"));
        log.info("Local:      http://localhost:{}{}", serverPort, contextPath);
        log.info("External:   {}", base);
        log.info("Swagger UI: {}", swagger);
        log.info("----------------------------------------------------------");
    }
}
