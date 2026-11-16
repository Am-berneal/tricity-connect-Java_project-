package com.tricityconnect.web;

import com.tricityconnect.model.AppUser;
import com.tricityconnect.service.UserService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Looks up the logged-in user (the session stores their mobile number). */
@Component
public class CurrentUser {

    private final UserService userService;

    public CurrentUser(UserService userService) {
        this.userService = userService;
    }

    public Optional<AppUser> get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return userService.findByMobile(auth.getName());
    }

    public AppUser require() {
        return get().orElseThrow(() -> new IllegalStateException("Not logged in"));
    }
}
