package com.example.backend.controller;

import com.example.backend.entity.User;
import com.example.backend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // REGISTER
    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user, HttpSession session) {

        User registeredUser = authService.register(user);
        session.setAttribute("EDUADAPT_USER_ID", registeredUser.getId());
        session.setAttribute("EDUADAPT_ROLE", registeredUser.getRole());

        return ResponseEntity.ok(registeredUser);
    }

    // LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData, HttpSession session) {

        String email = loginData.get("email");
        String password = loginData.get("password");

        User user = authService.login(email, password);

        if (user != null) {
            session.setAttribute("EDUADAPT_USER_ID", user.getId());
            session.setAttribute("EDUADAPT_ROLE", user.getRole());
            return ResponseEntity.ok(user);
        }

        return ResponseEntity
                .badRequest()
                .body("Invalid email or password");
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.noContent().build();
    }
}
