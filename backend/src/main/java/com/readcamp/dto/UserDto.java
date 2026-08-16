package com.readcamp.dto;

import com.readcamp.entity.User;
import lombok.Data;

/** 用户信息响应（不含密码，含学习聚合统计） */
@Data
public class UserDto {

    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private Integer role;
    private Boolean mustChangePassword;

    /** 精读完成文章数 */
    private long completedCount;
    /** 进行中文章数 */
    private long readingCount;
    /** 平均阅读进度 0-100 */
    private long totalProgress;

    public static UserDto from(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setRole(user.getRole());
        dto.setMustChangePassword(user.getMustChangePassword());
        return dto;
    }
}
