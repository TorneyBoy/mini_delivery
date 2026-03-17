package com.delivery.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求DTO
 */
@Data
@Schema(description = "修改密码请求")
public class ChangePasswordRequest {

    @Schema(description = "原密码", required = true)
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @Schema(description = "新密码", required = true)
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, message = "新密码至少6位")
    private String newPassword;
}