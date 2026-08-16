package com.readcamp.service;

import com.readcamp.dto.ChangePasswordRequest;
import com.readcamp.dto.LoginRequest;
import com.readcamp.dto.LoginResponse;
import com.readcamp.dto.RegisterRequest;
import com.readcamp.dto.UpdateProfileRequest;
import com.readcamp.dto.UserDto;

public interface UserService {

    /** 注册（用户名唯一），返回用户信息 */
    UserDto register(RegisterRequest request);

    /** 登录，返回 token + 用户信息 */
    LoginResponse login(LoginRequest request);

    /** 修改密码（校验旧密码；清除首登改密标记） */
    void changePassword(Long userId, ChangePasswordRequest request);

    /** 当前用户信息 */
    UserDto me(Long userId);

    /** 更新昵称/头像 */
    UserDto updateProfile(Long userId, UpdateProfileRequest request);
}
