package com.example.coresyncservice.service;

import com.example.coresyncservice.dto.AuthenticationRequest;
import com.example.coresyncservice.dto.AuthenticationResponse;
import com.example.coresyncservice.dto.RegisterRequest;
import com.example.coresyncservice.exception.UserNotFoundException;
import com.example.coresyncservice.model.Role;
import com.example.coresyncservice.model.User;
import com.example.coresyncservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User testUser;
    private RegisterRequest registerRequest;
    private AuthenticationRequest authenticationRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        registerRequest = RegisterRequest.builder()
                .name("Test User")
                .email("test@example.com")
                .password("rawPassword")
                .profilePictureUrl("http://example.com/pic.jpg")
                .build();

        authenticationRequest = AuthenticationRequest.builder()
                .email("test@example.com")
                .password("rawPassword")
                .build();
    }

    @Test
    void register_shouldReturnAuthenticationResponseWithToken() {
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwtToken");

        AuthenticationResponse response = authenticationService.register(registerRequest);

        assertNotNull(response);
        assertEquals("jwtToken", response.getToken());
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode("rawPassword");
        verify(jwtService, times(1)).generateToken(any(User.class));
    }

    @Test
    void authenticate_shouldReturnAuthenticationResponseWithToken() {
        when(userRepository.findByEmail(authenticationRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwtToken");

        AuthenticationResponse response = authenticationService.authenticate(authenticationRequest);

        assertNotNull(response);
        assertEquals("jwtToken", response.getToken());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, times(1)).findByEmail(authenticationRequest.getEmail());
        verify(jwtService, times(1)).generateToken(any(User.class));
    }

    @Test
    void authenticate_shouldThrowUserNotFoundException() {
        when(userRepository.findByEmail(authenticationRequest.getEmail())).thenReturn(Optional.empty());
        // Mock authenticationManager to not throw an exception here, as the service checks userRepository first
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);


        assertThrows(UserNotFoundException.class, () -> authenticationService.authenticate(authenticationRequest));

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, times(1)).findByEmail(authenticationRequest.getEmail());
        verify(jwtService, never()).generateToken(any(User.class));
    }
}
