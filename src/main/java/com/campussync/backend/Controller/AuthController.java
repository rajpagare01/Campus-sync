package com.campussync.backend.Controller;

import com.campussync.backend.Dto.AuthResponse;
import com.campussync.backend.Dto.LoginRequest;
import com.campussync.backend.Dto.LogoutRequest;
import com.campussync.backend.Dto.OtpInitiationResponse;
import com.campussync.backend.Dto.RefreshTokenRequest;
import com.campussync.backend.Dto.RegisterRequest;
import com.campussync.backend.Dto.VerifyRequest;
import com.campussync.backend.Service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/auth", "/api/v1/auth"})
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public OtpInitiationResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/verify")
    public AuthResponse verify(@Valid @RequestBody VerifyRequest request) {
        return userService.verify(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @PostMapping("/refresh-token")
    public AuthResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return userService.refreshToken(request);
    }

    @PostMapping("/logout")
    public String logout(@RequestBody(required = false) LogoutRequest request) {
        return userService.logout(request);
    }

    @PostMapping("/resend-otp")
    public String resendOtp(@RequestParam String email) {
        return userService.resendOtp(email);
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email) {
        return userService.forgotPassword(email);
    }

    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam String email,
            @RequestParam String otp,
            @RequestParam String newPassword
    ) {
        return userService.resetPassword(email, otp, newPassword);
    }
}
