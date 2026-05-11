package com.example.coresyncservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

/**
 * Initialises the Firebase Admin SDK once on application startup.
 *
 * <p>Two credential strategies are supported, evaluated in order:
 * <ol>
 *   <li><b>Base64 env var</b> — set {@code FIREBASE_SERVICE_ACCOUNT_BASE64} to the
 *       base64-encoded contents of your service-account JSON. This is the preferred
 *       approach for Docker / CI deployments where mounting files is inconvenient.
 *       Generate with: {@code base64 -w 0 serviceAccountKey.json}</li>
 *   <li><b>File path</b> — set {@code FIREBASE_SERVICE_ACCOUNT_PATH} to the absolute
 *       path of the service-account JSON file on the host. Useful for local dev.</li>
 * </ol>
 *
 * <p>ACTION REQUIRED: Download your Firebase service-account key from
 * Firebase Console → Project Settings → Service Accounts → Generate new private key.
 * Then either base64-encode it and store it in {@code FIREBASE_SERVICE_ACCOUNT_BASE64},
 * or place the file somewhere on disk and set {@code FIREBASE_SERVICE_ACCOUNT_PATH}.
 * Never commit the raw JSON file to version control.
 *
 * <p>ACTION REQUIRED: Set {@code FIREBASE_STORAGE_BUCKET} to your Firebase Storage
 * bucket name, which looks like {@code your-project-id.appspot.com}.
 * Find it in Firebase Console → Storage → Files (shown in the top bar).
 */
// @Configuration
@Component
public class FirebaseConfig implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    /**
     * Base64-encoded service-account JSON.
     * Takes priority over {@code FIREBASE_SERVICE_ACCOUNT_PATH} when both are set.
     */
    @Value("${firebase.service-account.base64:}")
    private String serviceAccountBase64;

    /** Absolute path to the service-account JSON file on disk. */
    @Value("${firebase.service-account.path:}")
    private String serviceAccountPath;

    /** Classpath resource name pointing at a local Firebase service account json file. */
    @Value("${firebase.service-account.resource:}")
    private String serviceAccountResource;

    /**
     * Firebase Storage bucket name, e.g. {@code your-project-id.appspot.com}.
     *
     * ACTION REQUIRED: Set this to your actual bucket name.
     */
    @Value("${firebase.storage.bucket:}")
    private String storageBucket;

    public FirebaseConfig() {
        log.info("FirebaseConfig bean is being created");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initializeFirebase();
    }

    private void initializeFirebase() {
        try {
            log.info("initializeFirebase method called");
            // Skip if already initialised (e.g. in test contexts)
            if (!FirebaseApp.getApps().isEmpty()) {
                log.info("Firebase already initialised — skipping.");
                return;
            }

            log.info("serviceAccountBase64: {}", serviceAccountBase64 != null ? "set (length: " + serviceAccountBase64.length() + ")" : "null");
            log.info("serviceAccountPath: {}", serviceAccountPath);
            log.info("serviceAccountResource: {}", serviceAccountResource);
            log.info("storageBucket: {}", storageBucket);

            log.info("Attempting to initialize Firebase Admin SDK...");
            GoogleCredentials credentials = resolveCredentials();
            log.info("Successfully loaded Firebase credentials.");

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setStorageBucket(storageBucket)
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK initialised successfully. Storage bucket: {}", storageBucket);
        } catch (Exception ex) {
            log.error("Failed to initialize Firebase Admin SDK: {}", ex.getMessage(), ex);
            throw new RuntimeException("Firebase initialization failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Resolves Firebase credentials from the configured source.
     *
     * @throws IllegalStateException if neither credential source is configured
     */
    private GoogleCredentials resolveCredentials() throws IOException {
        // Strategy 1: base64-encoded env var (preferred for containerised envs)
        if (serviceAccountBase64 != null && !serviceAccountBase64.isBlank()) {
            log.info("Loading Firebase credentials from base64 environment variable.");
            byte[] decoded = Base64.getDecoder().decode(serviceAccountBase64.trim());
            try (InputStream stream = new ByteArrayInputStream(decoded)) {
                return GoogleCredentials.fromStream(stream);
            }
        }

        // Strategy 2: file path
        if (serviceAccountPath != null && !serviceAccountPath.isBlank()) {
            log.info("Loading Firebase credentials from file: {}", serviceAccountPath);
            try (InputStream stream = new FileInputStream(serviceAccountPath)) {
                return GoogleCredentials.fromStream(stream);
            }
        }

        // Strategy 3: classpath resource
        Resource resource = resolveClasspathResource();
        if (resource != null && resource.exists()) {
            log.info("Loading Firebase credentials from classpath resource: {}", resource.getFilename());
            try (InputStream stream = resource.getInputStream()) {
                return GoogleCredentials.fromStream(stream);
            }
        }

        throw new IllegalStateException(
                "Firebase credentials are not configured. " +
                "Set either FIREBASE_SERVICE_ACCOUNT_BASE64 (recommended), " +
                "FIREBASE_SERVICE_ACCOUNT_PATH or FIREBASE_SERVICE_ACCOUNT_RESOURCE in your environment.");
    }

    private Resource resolveClasspathResource() {
        if (serviceAccountResource != null && !serviceAccountResource.isBlank()) {
            log.info("Loading Firebase credentials from classpath resource: {}", serviceAccountResource);
            ClassPathResource resource = new ClassPathResource(serviceAccountResource);
            if (resource.exists()) {
                return resource;
            } else {
                log.warn("Classpath resource not found: {}", serviceAccountResource);
            }
        }

        ClassPathResource defaultResource = new ClassPathResource("medihafi-firebase-adminsdk-fbsvc-e3da1eb8cc.json");
        if (defaultResource.exists()) {
            log.info("Using default Firebase service account from classpath: medihafi-firebase-adminsdk-fbsvc-e3da1eb8cc.json");
            return defaultResource;
        } else {
            log.warn("Default Firebase service account not found in classpath: medihafi-firebase-adminsdk-fbsvc-e3da1eb8cc.json");
        }

        return null;
    }
}
