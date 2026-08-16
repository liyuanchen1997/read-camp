package com.readcamp.controller;

import com.readcamp.common.Result;
import com.readcamp.common.UserContext;
import com.readcamp.dto.ChangePasswordRequest;
import com.readcamp.dto.LoginRequest;
import com.readcamp.dto.LoginResponse;
import com.readcamp.dto.RegisterRequest;
import com.readcamp.dto.UserDto;
import com.readcamp.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<UserDto> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(userService.register(request));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(userService.login(request));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(UserContext.userId(), request);
        return Result.ok();
    }
}
