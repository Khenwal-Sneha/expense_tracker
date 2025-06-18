package com.example.tracker.service;

import com.example.tracker.model.Expense;
import com.example.tracker.model.User;
import com.example.tracker.repository.ExpenseRepository;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    public List<Expense> getExpensesByUser(User user) {
        return expenseRepository.findByUser(user);
    }

    public Expense getById(Long id) {
        return expenseRepository.findById(id).orElse(null);
    }

    public void save(Expense expense) {
        expenseRepository.save(expense);
    }

    public void delete(Long id) {
        expenseRepository.deleteById(id);
    }

    public Double getMonthlyTotal() {
        return expenseRepository.getMonthlyTotal();
    }

    public Double getWeeklyTotal() {
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
        return expenseRepository.getWeeklyTotal(sevenDaysAgo);
    }

    public Map<String, Double> getCategoryWiseTotals() {
        List<Object[]> results = expenseRepository.getTotalByCategory();
        Map<String, Double> categoryTotals = new HashMap<>();
        for (Object[] row : results) {
            categoryTotals.put((String) row[0], (Double) row[1]);
        }
        return categoryTotals;
    }

    public List<Expense> getFilteredExpenses(String email, String category, LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null && (category == null || category.isBlank())) {
            return expenseRepository.findByUserEmail(email);
        }

        return expenseRepository.findFiltered(email, category, startDate, endDate);
    }

    public List<Expense> getExpensesByCategory(String email, String customCategory, LocalDate startDate, LocalDate endDate) {
        return expenseRepository.findFiltered(email, customCategory, startDate, endDate);
    }
}
