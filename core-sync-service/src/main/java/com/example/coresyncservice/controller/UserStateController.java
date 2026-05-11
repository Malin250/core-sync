package com.example.coresyncservice.controller;

import com.example.coresyncservice.dto.UserStateDto;
import com.example.coresyncservice.service.UserStateService;
import jakarta.validation.Valid; // Import Valid
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/state")
@RequiredArgsConstructor
public class UserStateController {

    private final UserStateService userStateService;

    @GetMapping
    public ResponseEntity<UserStateDto> getUserState() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = ((UserDetails) authentication.getPrincipal()).getUsername();

        Optional<UserStateDto> userState = userStateService.getUserState(userEmail);
        return userState.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping
    public ResponseEntity<UserStateDto> updateUserState(@Valid @RequestBody UserStateDto userStateDto) { // Added @Valid
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = ((UserDetails) authentication.getPrincipal()).getUsername();

        UserStateDto updatedState = userStateService.updateUserState(userEmail, userStateDto);
        return ResponseEntity.ok(updatedState);
    }
}
