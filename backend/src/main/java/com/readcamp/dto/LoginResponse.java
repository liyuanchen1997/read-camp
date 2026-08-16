package com.readcamp.dto;

import lombok.Data;

/** 登录响应 */
@Data
public class LoginResponse {

    private String token;
    private UserDto user;
}
