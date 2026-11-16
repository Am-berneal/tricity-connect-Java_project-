package com.tricityconnect.web;

import com.tricityconnect.dto.RegistrationForm;
import com.tricityconnect.service.BookingException;
import com.tricityconnect.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class AuthController {

    public static final List<String> CITIES = List.of("Chandigarh", "Mohali", "Panchkula", "Zirakpur",
            "Kharar", "New Chandigarh", "Other");

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegistrationForm());
        model.addAttribute("cities", CITIES);
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form, BindingResult result, Model model) {
        model.addAttribute("cities", CITIES);
        if (form.getConfirmPassword() != null && !form.getConfirmPassword().equals(form.getPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }
        if (result.hasErrors()) {
            return "register";
        }
        try {
            userService.register(form);
        } catch (BookingException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
        return "redirect:/login?registered";
    }
}
