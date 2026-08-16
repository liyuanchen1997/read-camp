package com.readcamp.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 更新个人资料请求（昵称/头像） */
@Data
public class UpdateProfileRequest {

    @Size(max = 20, message = "昵称最长 20 字符")
    private String nickname;

    @Size(max = 255, message = "头像 URL 过长")
    private String avatarUrl;
}
