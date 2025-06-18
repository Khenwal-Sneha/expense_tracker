package com.example.tracker.controller;

import com.example.tracker.export.ExcelExporter;
import com.example.tracker.model.BudgetHistory;
import com.example.tracker.model.Expense;
import com.example.tracker.model.User;
import com.example.tracker.repository.BudgetHistoryRepository;
import com.example.tracker.repository.UserRepository;
import com.example.tracker.service.ExpenseService;
import com.example.tracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.apache.commons.io.IOUtils;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;



import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class ExpenseController {
    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    // Dashboard view
    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        model.addAttribute("monthlyTotal", expenseService.getMonthlyTotal());
        model.addAttribute("weeklyTotal", expenseService.getWeeklyTotal());

        Map<String, Double> categoryTotals = expenseService.getCategoryWiseTotals();
        model.addAttribute("categoryLabels", categoryTotals.keySet());
        model.addAttribute("categoryData", categoryTotals.values());

        return "dashboard";
    }
    @GetMapping("/expenses")
    public String viewDashboard(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String customCategory,
            Model model,
            Principal principal) {

        User user = userService.findByEmail(principal.getName());
        String email = user.getEmail();

        List<Expense> expenses;

        if ("Other".equals(category) && customCategory != null && !customCategory.isBlank()) {
            expenses = expenseService.getFilteredExpenses(email, customCategory, startDate, endDate);
        } else {
            expenses = expenseService.getFilteredExpenses(email, category, startDate, endDate);
        }

        // Calculate total spent in current month
        YearMonth currentMonth = YearMonth.now();
        double totalThisMonth = expenses.stream()
                .filter(e -> YearMonth.from(e.getDate()).equals(currentMonth))
                .mapToDouble(Expense::getAmount)
                .sum();

        Double budget = user.getMonthlyBudget();
        double usagePercent = (budget != null && budget > 0) ? (totalThisMonth / budget) * 100 : 0;

        model.addAttribute("expenses", expenses);
        model.addAttribute("monthlySpent", totalThisMonth);
        model.addAttribute("budget", budget);
        model.addAttribute("usagePercent", usagePercent);

        // Pass back filter values to retain them in the form
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("category", category);
        model.addAttribute("customCategory", customCategory);

        return "expenses"; // Assuming "expenses.html" is your dashboard
    }



    // Show add form
    @GetMapping("/expenses/add")
    public String addExpenseForm(Model model) {
        model.addAttribute("expense", new Expense());
        return "expenseForm";
    }

    // Save expense
    @PostMapping("/expenses/save")
    public String saveExpense(@ModelAttribute Expense expense,
                              @RequestParam(value = "customCategory", required = false) String customCategory,
                              Principal principal) {
        String email = principal.getName();
        User user = userService.findByEmail(email);
        expense.setUser(user);
    
        // If "Other" is selected, use the custom category
        if ("Other".equals(expense.getCategory()) && customCategory != null && !customCategory.trim().isEmpty()) {
            expense.setCategory(customCategory.trim());
        }
    
        // If no date is set, use today's date
        if (expense.getDate() == null) {
            expense.setDate(LocalDate.now());
        }
    
        expenseService.save(expense);
        return "redirect:/expenses";
    }
    

    // Edit
    @GetMapping("/expenses/edit/{id}")
    public String editExpense(@PathVariable Long id, Model model) {
        Expense expense = expenseService.getById(id);
        model.addAttribute("expense", expense);
        return "expense-form";
    }

    // Delete
    @GetMapping("/expenses/delete/{id}")
    public String deleteExpense(@PathVariable Long id) {
        expenseService.delete(id);
        return "redirect:/expenses";
    }
    @GetMapping("/expenses/export/excel")
    public void exportToExcel(HttpServletResponse response, Principal principal) throws IOException {
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=expenses.xlsx");

        // Fetch user using principal
        User user = userService.findByEmail(principal.getName());
        List<Expense> expenses = expenseService.getExpensesByUser(user);

        ByteArrayInputStream stream = ExcelExporter.expensesToExcel(expenses);
        IOUtils.copy(stream, response.getOutputStream());
    }

    @GetMapping("/expenses/chart")
    public String showCategoryChart(Model model, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        List<Expense> expenses = expenseService.getExpensesByUser(user);

        // Group by category and sum amounts
        Map<String, Double> categoryTotals = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.summingDouble(Expense::getAmount)
                ));

        // Sort and take top 5
        List<Map.Entry<String, Double>> top5 = categoryTotals.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .toList();

        // Split into keys and values for the chart
        List<String> categories = top5.stream().map(Map.Entry::getKey).toList();
        List<Double> amounts = top5.stream().map(Map.Entry::getValue).toList();

        model.addAttribute("categories", categories);
        model.addAttribute("amounts", amounts);

        return "chart"; // the name of the Thymeleaf view (chart.html)
    }

    @GetMapping("/expenses/trend")
    public String showMonthlyTrend(Model model, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        List<Expense> expenses = expenseService.getExpensesByUser(user);

        // Initialize month totals
        Map<String, Double> monthlyTotals = new LinkedHashMap<>();
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        for (String month : months) {
            monthlyTotals.put(month, 0.0);
        }

        for (Expense expense : expenses) {
            int monthIndex = expense.getDate().getMonthValue() - 1; // 0-based index
            String monthName = months[monthIndex];
            monthlyTotals.put(monthName,
                    monthlyTotals.get(monthName) + expense.getAmount());
        }

        model.addAttribute("months", monthlyTotals.keySet());
        model.addAttribute("amounts", monthlyTotals.values());

        return "trend"; // Thymeleaf template name
    }
    @Autowired
    private BudgetHistoryRepository budgetHistoryRepository;

    @PostMapping("/profile/update-budget")
    public String updateBudget(@RequestParam("budget") Double budget, Principal principal) {
        User user = userService.findByEmail(principal.getName());

        // Save new history record
        BudgetHistory history = new BudgetHistory();
        history.setUser(user);
        history.setBudget(budget);
        history.setUpdatedDate(LocalDate.now());
        budgetHistoryRepository.save(history);

        // Update current user object
        user.setMonthlyBudget(budget);
        userRepository.save(user);

        return "redirect:/profile";
    }

    @GetMapping("/guide")
    public String guide(){
        return "guide";
    }
}
