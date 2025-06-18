package com.example.tracker.repository;

import com.example.tracker.model.Expense;
import com.example.tracker.model.User;
import jakarta.persistence.TypedQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUser(User user);

    List<Expense> findByUserEmail(String email);

    List<Expense> findByUserAndDateBetween(User user, LocalDate start, LocalDate end);

    List<Expense> findByUserAndDateBetweenAndCategoryContainingIgnoreCase(
            User user, LocalDate start, LocalDate end, String category
    );

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE MONTH(e.date) = MONTH(CURRENT_DATE) AND YEAR(e.date) = YEAR(CURRENT_DATE)")
    Double getMonthlyTotal();

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.date >= :fromDate")
    Double getWeeklyTotal(@Param("fromDate") LocalDate fromDate);

    @Query("SELECT e.category, SUM(e.amount) FROM Expense e GROUP BY e.category")
    List<Object[]> getTotalByCategory();

    // ✅ NEW Flexible Filtering for Dashboard
    @Query("SELECT e FROM Expense e WHERE e.user.email = :email " +
            "AND (:category IS NULL OR LOWER(e.category) = LOWER(:category)) " +
            "AND (:startDate IS NULL OR e.date >= :startDate) " +
            "AND (:endDate IS NULL OR e.date <= :endDate)")
    List<Expense> findFiltered(
            @Param("email") String email,
            @Param("category") String category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
