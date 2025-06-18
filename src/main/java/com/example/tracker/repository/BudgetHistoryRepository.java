package com.example.tracker.repository;

import com.example.tracker.model.BudgetHistory;
import com.example.tracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BudgetHistoryRepository extends JpaRepository<BudgetHistory, Long> {
    List<BudgetHistory> findByUserOrderByUpdatedDateDesc(User user);
}
