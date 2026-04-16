package com.run.handler.role.vo;

import com.run.auth.constants.PermissionConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
public class CreateRoleVO {
    @NotBlank(message = "角色名称不能为空")
    private String name;
    @NotNull(message = "继承角色不能为空")
    private PermissionConstants.Role type;
}
