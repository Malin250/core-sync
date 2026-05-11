package com.example.coresyncservice.repository;

import com.example.coresyncservice.model.User;
import com.example.coresyncservice.model.UserState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserStateRepository extends JpaRepository<UserState, Long> {
    Optional<UserState> findByUser(User user);
}
