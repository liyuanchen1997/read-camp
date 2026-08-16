package com.readcamp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.readcamp.common.ApiException;
import com.readcamp.common.JwtUtil;
import com.readcamp.dto.ChangePasswordRequest;
import com.readcamp.dto.LoginRequest;
import com.readcamp.dto.LoginResponse;
import com.readcamp.dto.RegisterRequest;
import com.readcamp.dto.UpdateProfileRequest;
import com.readcamp.dto.UserDto;
import com.readcamp.entity.User;
import com.readcamp.mapper.UserMapper;
import com.readcamp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public UserDto register(RegisterRequest request) {
        String username = request.getUsername().trim();
        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (exists > 0) {
            throw new ApiException(400, 40002, "用户名已被占用");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname())
                ? request.getNickname().trim() : username);
        user.setAvatarUrl("");
        user.setRole(0);
        user.setMustChangePassword(false);
        user.setStatus(1);
        userMapper.insert(user);
        return UserDto.from(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername().trim()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException(400, 40003, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new ApiException(403, 40301, "账号已被禁用");
        }

        LoginResponse response = new LoginResponse();
        response.setToken(jwtUtil.generate(user.getId(), user.getUsername(), user.getRole()));
        response.setUser(UserDto.from(user));
        return response;
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw ApiException.notFound("用户不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new ApiException(400, 40004, "旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        // 首登强制改密完成后清除标记
        if (Boolean.TRUE.equals(user.getMustChangePassword())) {
            user.setMustChangePassword(false);
        }
        userMapper.updateById(user);
    }

    @Override
    public UserDto me(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw ApiException.notFound("用户不存在");
        }
        return UserDto.from(user);
    }

    @Override
    public UserDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw ApiException.notFound("用户不存在");
        }
        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname().trim());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }
        userMapper.updateById(user);
        return UserDto.from(user);
    }
}
