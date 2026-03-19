package com.lk.microservicio_seguridad.Controllers;

import com.lk.microservicio_seguridad.models.User;
import com.lk.microservicio_seguridad.Services.AuthService;
import com.lk.microservicio_seguridad.Services.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final NotificationController notificationController;

    public AuthController(AuthService authService, JwtService jwtService, NotificationController notificationController) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.notificationController = notificationController;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String email = request.get("email");
        String password = request.get("password");

        User user = authService.register(name, email, password);
        notificationController.sendWelcomeEmail(user);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        User user = authService.login(email, password);
        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(Map.of("token", token));
    }
}