package com.tricityconnect.service;

import com.tricityconnect.dto.RegistrationForm;
import com.tricityconnect.model.AppUser;
import com.tricityconnect.model.Role;
import com.tricityconnect.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Registration and login. Users can log in with their mobile number or email.
 * Passwords are stored as BCrypt hashes, never as plain text.
 */
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        AppUser u = findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + login));
        // The mobile number is used as the username inside the session
        return User.withUsername(u.getMobile())
                .password(u.getPasswordHash())
                .roles(u.getRole().name())
                .build();
    }

    public Optional<AppUser> findByLogin(String login) {
        if (login == null) {
            return Optional.empty();
        }
        String value = login.trim().replace("+91", "").replace(" ", "");
        Optional<AppUser> byMobile = userRepository.findByMobile(value);
        return byMobile.isPresent() ? byMobile : userRepository.findByEmailIgnoreCase(login.trim());
    }

    public Optional<AppUser> findByMobile(String mobile) {
        return userRepository.findByMobile(mobile);
    }

    @Transactional
    public AppUser updateProfile(AppUser user, String fullName, String email, String homeCity) {
        AppUser u = userRepository.findById(user.getId()).orElseThrow();
        if (fullName == null || fullName.isBlank()) {
            throw new BookingException("Name cannot be empty.");
        }
        String mail = email == null || email.isBlank() ? null : email.trim();
        if (mail != null && !mail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BookingException("Enter a valid email address.");
        }
        if (mail != null && !mail.equalsIgnoreCase(u.getEmail()) && userRepository.existsByEmailIgnoreCase(mail)) {
            throw new BookingException("This email is used by another account.");
        }
        u.setFullName(fullName.trim());
        u.setEmail(mail);
        u.setHomeCity(homeCity);
        return u;
    }

    @Transactional
    public void changePassword(AppUser user, String current, String newPassword, String confirm) {
        AppUser u = userRepository.findById(user.getId()).orElseThrow();
        if (!passwordEncoder.matches(current == null ? "" : current, u.getPasswordHash())) {
            throw new BookingException("Your current password is wrong.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new BookingException("New password must be at least 8 characters.");
        }
        if (!newPassword.equals(confirm)) {
            throw new BookingException("New passwords do not match.");
        }
        u.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    @Transactional
    public AppUser updateEmergencyContact(AppUser user, String name, String mobile) {
        AppUser u = userRepository.findById(user.getId()).orElseThrow();
        String m = mobile == null ? "" : mobile.replace("+91", "").replace(" ", "");
        if (!m.isEmpty() && !m.matches("[6-9]\\d{9}")) {
            throw new BookingException("Enter a valid 10-digit mobile number.");
        }
        u.setEmergencyName(name == null || name.isBlank() ? null : name.trim());
        u.setEmergencyMobile(m.isEmpty() ? null : m);
        return u;
    }

    @Transactional
    public AppUser register(RegistrationForm form) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new BookingException("Passwords do not match.");
        }
        if (userRepository.existsByMobile(form.getMobile())) {
            throw new BookingException("An account with this mobile number already exists. Please log in.");
        }
        String email = form.getEmail() == null || form.getEmail().isBlank() ? null : form.getEmail().trim();
        if (email != null && userRepository.existsByEmailIgnoreCase(email)) {
            throw new BookingException("An account with this email already exists. Please log in.");
        }
        AppUser u = new AppUser(form.getFullName().trim(), form.getMobile(), email, form.getGender(),
                form.getHomeCity(), passwordEncoder.encode(form.getPassword()), Role.PASSENGER);
        // Welcome bonus so new users can try wallet payment
        u.setWalletBalance(new BigDecimal("50.00"));
        return userRepository.save(u);
    }
}
