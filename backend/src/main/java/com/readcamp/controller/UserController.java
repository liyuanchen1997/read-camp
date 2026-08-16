package com.readcamp.controller;

import com.readcamp.common.Result;
import com.readcamp.common.UserContext;
import com.readcamp.dto.RecentReadingItem;
import com.readcamp.dto.UpdateProfileRequest;
import com.readcamp.dto.UserDto;
import com.readcamp.service.ProgressService;
import com.readcamp.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ProgressService progressService;

    /** 当前用户资料（含聚合统计：精读完成数/进行中/平均进度） */
    @GetMapping("/me")
    public Result<UserDto> me() {
        return Result.ok(userService.me(UserContext.userId()));
    }

    /** 近期阅读（按最近阅读时间倒序，带进度条数据） */
    @GetMapping("/me/recent-reading")
    public Result<List<RecentReadingItem>> recentReading() {
        return Result.ok(progressService.recentReading(UserContext.userId()));
    }

    @PutMapping("/me")
    public Result<UserDto> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.ok(userService.updateProfile(UserContext.userId(), request));
    }
}
