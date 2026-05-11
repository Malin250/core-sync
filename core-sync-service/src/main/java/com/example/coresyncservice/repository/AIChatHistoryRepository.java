package com.example.coresyncservice.repository;

import com.example.coresyncservice.model.AIChatHistory;
import com.example.coresyncservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AIChatHistoryRepository extends JpaRepository<AIChatHistory, Long> {
    List<AIChatHistory> findByUserOrderByTimestampAsc(User user);
}
