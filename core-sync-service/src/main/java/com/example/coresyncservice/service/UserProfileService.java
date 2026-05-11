package com.example.coresyncservice.service;

import com.example.coresyncservice.dto.UserProfileDto;
import com.example.coresyncservice.exception.InvalidFileException;
import com.example.coresyncservice.exception.UserNotFoundException;
import com.example.coresyncservice.model.User;
import com.example.coresyncservice.repository.UserRepository;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.firebase.cloud.StorageClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Manages user profile data and Firebase Storage-backed profile picture uploads.
 *
 * <p>Uploaded images are stored under the {@code profile-pictures/} prefix inside
 * your Firebase Storage bucket and made publicly readable via a bucket-level ACL.
 *
 * <p>ACTION REQUIRED: In the Firebase Console → Storage → Rules, ensure your
 * bucket rules allow public reads for the {@code profile-pictures/} path, or
 * restrict access appropriately for your security model.
 */
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

    /** Allowed MIME types for profile picture uploads. */
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    /** Maximum profile-picture size: 5 MB. */
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private final UserRepository userRepository;

    /**
     * Firebase Storage bucket name, e.g. {@code your-project-id.appspot.com}.
     * Must match the value set in {@code FirebaseConfig} / {@code FIREBASE_STORAGE_BUCKET}.
     */
    @Value("${firebase.storage.bucket}")
    private String storageBucket;

    // ── Profile read / update ─────────────────────────────────

    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(String userEmail) {
        return toDto(requireUser(userEmail));
    }

    @Transactional
    public UserProfileDto updateUserProfile(String userEmail, UserProfileDto dto) {
        User user = requireUser(userEmail);
        user.setName(dto.getName());

        // Only overwrite the picture URL if one was explicitly provided
        if (dto.getProfilePictureUrl() != null && !dto.getProfilePictureUrl().isBlank()) {
            user.setProfilePictureUrl(dto.getProfilePictureUrl());
        }

        return toDto(userRepository.save(user));
    }

    // ── Profile picture upload ────────────────────────────────

    /**
     * Validates the uploaded file, stores it in Firebase Storage under
     * {@code profile-pictures/<uuid><ext>}, and updates the user's record
     * with the resulting public download URL.
     */
    @Transactional
    public UserProfileDto uploadProfilePicture(String userEmail, MultipartFile file) {
        User user = requireUser(userEmail);
        validateFile(file);

        try {
            String extension  = extractExtension(file.getOriginalFilename());
            String blobPath   = "profile-pictures/" + UUID.randomUUID() + extension;
            String contentType = file.getContentType();

            // Build the object metadata
            BlobInfo blobInfo = BlobInfo.newBuilder(storageBucket, blobPath)
                    .setContentType(contentType)
                    // Make the uploaded object publicly readable so the app can display it
                    .setAcl(List.of(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)))
                    .build();

            // Upload via the Firebase Admin Storage client
            Storage storage = StorageClient.getInstance().bucket().getStorage();
            Blob blob = storage.create(blobInfo, file.getBytes());

            // Construct the public download URL
            String publicUrl = buildFirebaseUrl(blobPath);
            user.setProfilePictureUrl(publicUrl);
            log.info("Profile picture uploaded to Firebase for user {}: {}", userEmail, publicUrl);

            return toDto(userRepository.save(user));

        } catch (IOException ex) {
            throw new InvalidFileException("Failed to read uploaded file: " + file.getOriginalFilename(), ex);
        } catch (Exception ex) {
            log.error("Firebase Storage upload failed for user {}: {}", userEmail, ex.getMessage());
            throw new InvalidFileException("Failed to upload file to storage. Please try again.");
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileException("File must not be empty.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidFileException(String.format(
                    "File size exceeds the %d MB limit.", MAX_FILE_SIZE_BYTES / (1024 * 1024)));
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidFileException(
                    "Unsupported file type. Allowed types: JPEG, PNG, WebP, GIF.");
        }
    }

    private String extractExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.'));
        }
        return "";
    }

    /**
     * Builds the public Firebase Storage download URL for a stored object.
     *
     * <p>Pattern: {@code https://firebasestorage.googleapis.com/v0/b/{bucket}/o/{encoded-path}?alt=media}
     */
    private String buildFirebaseUrl(String blobPath) {
        String encoded = blobPath.replace("/", "%2F");
        return String.format(
                "https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                storageBucket, encoded);
    }

    private UserProfileDto toDto(User user) {
        return UserProfileDto.builder()
                .email(user.getEmail())
                .name(user.getName())
                .profilePictureUrl(user.getProfilePictureUrl())
                .build();
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + email));
    }
}

