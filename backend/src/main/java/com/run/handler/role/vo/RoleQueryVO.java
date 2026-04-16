package com.run.handler.role.vo;

import com.run.common.query.vo.BasePageQueryVO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RoleQueryVO extends BasePageQueryVO {
    private String name;
}
