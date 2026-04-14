package com.run.handler.user.vo;

import com.run.common.query.annotations.QueryParams;
import com.run.common.query.vo.BasePageQueryVO;
import lombok.Getter;
import lombok.Setter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/14  21:24}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
public class UserQueryVO extends BasePageQueryVO {
    @QueryParams(name = "global")
    private String global;
    @QueryParams(name = "username")
    private String username;
    @QueryParams(name = "nickname")
    private String nickname;
}
