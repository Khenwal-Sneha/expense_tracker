package com.example.tracker.controller;

import com.example.tracker.model.BudgetHistory;
import com.example.tracker.model.User;
import com.example.tracker.repository.BudgetHistoryRepository;
import com.example.tracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private BudgetHistoryRepository budgetHistoryRepository;

    @GetMapping("/")
public String home() {
    return "home";
}


    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, Model model) {
        if (userService.findByEmail(user.getEmail()) != null) {
            model.addAttribute("error", "Email already registered!");
            return "register";
        }
        userService.registerUser(user);
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @GetMapping("/profile")
    public String viewProfile(Model model, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        model.addAttribute("user", user);

        List<BudgetHistory> historyList = budgetHistoryRepository.findByUserOrderByUpdatedDateDesc(user);
        model.addAttribute("budgetHistory", historyList);
        return "profile"; // profile.html view
    }
}
