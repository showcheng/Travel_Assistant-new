package com.travel.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("user")
@Schema(description = "用户")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "手机号")
    @TableField("phone")
    private String phone;

    @Schema(description = "密码（加密）")
    @TableField("password_hash")
    private String passwordHash;

    @Schema(description = "昵称")
    @TableField("nickname")
    private String nickname;

    @Schema(description = "头像 URL")
    @TableField("avatar_url")
    private String avatarUrl;

    @Schema(description = "注册来源")
    @TableField("register_source")
    private String registerSource;

    @Schema(description = "账号状态：0-正常，1-锁定")
    @TableField("status")
    private Integer status;

    @Schema(description = "逻辑删除：0-未删除，1-已删除")
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
