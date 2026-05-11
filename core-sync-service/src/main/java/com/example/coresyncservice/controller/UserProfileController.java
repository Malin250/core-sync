package com.example.coresyncservice.controller;

import com.example.coresyncservice.dto.UserProfileDto;
import com.example.coresyncservice.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    public ResponseEntity<UserProfileDto> getUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = ((UserDetails) authentication.getPrincipal()).getUsername();

        UserProfileDto userProfile = userProfileService.getUserProfile(userEmail);
        return ResponseEntity.ok(userProfile);
    }

    @PutMapping
    public ResponseEntity<UserProfileDto> updateUserProfile(@Valid @RequestBody UserProfileDto userProfileDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = ((UserDetails) authentication.getPrincipal()).getUsername();

        UserProfileDto updatedProfile = userProfileService.updateUserProfile(userEmail, userProfileDto);
        return ResponseEntity.ok(updatedProfile);
    }

    @PostMapping("/picture")
    public ResponseEntity<UserProfileDto> uploadProfilePicture(@RequestParam("file") MultipartFile file) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = ((UserDetails) authentication.getPrincipal()).getUsername();

        UserProfileDto updatedProfile = userProfileService.uploadProfilePicture(userEmail, file);
        return ResponseEntity.ok(updatedProfile);
    }
}
