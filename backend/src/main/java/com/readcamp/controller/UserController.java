package com.readcamp.controller;

import com.readcamp.common.Result;
import com.readcamp.common.UserContext;
import com.readcamp.dto.UpdateProfileRequest;
import com.readcamp.dto.UserDto;
import com.readcamp.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 当前用户资料（步骤 5 将补充聚合统计：精读文章数/总进度） */
    @GetMapping("/me")
    public Result<UserDto> me() {
        return Result.ok(userService.me(UserContext.userId()));
    }

    @PutMapping("/me")
    public Result<UserDto> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.ok(userService.updateProfile(UserContext.userId(), request));
    }
}
