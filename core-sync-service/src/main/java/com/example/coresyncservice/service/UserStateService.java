package com.example.coresyncservice.service;

import com.example.coresyncservice.dto.UserStateDto;
import com.example.coresyncservice.exception.UserNotFoundException; // Import the custom exception
import com.example.coresyncservice.model.User;
import com.example.coresyncservice.model.UserState;
import com.example.coresyncservice.repository.UserRepository;
import com.example.coresyncservice.repository.UserStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserStateService {

    private final UserStateRepository userStateRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Optional<UserStateDto> getUserState(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail)); // Use custom exception

        return userStateRepository.findByUser(user)
                .map(userState -> UserStateDto.builder().stateJson(userState.getStateJson()).build());
    }

    @Transactional
    public UserStateDto updateUserState(String userEmail, UserStateDto userStateDto) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail)); // Use custom exception

        UserState userState = userStateRepository.findByUser(user)
                .orElseGet(() -> UserState.builder().user(user).build());

        userState.setStateJson(userStateDto.getStateJson());
        UserState savedState = userStateRepository.save(userState);

        return UserStateDto.builder().stateJson(savedState.getStateJson()).build();
    }
}
