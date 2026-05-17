package com.run.handler.conversation.vo;

import com.run.common.query.annotations.QueryParams;
import com.run.common.query.vo.BasePageQueryVO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ApplicationQueryVO extends BasePageQueryVO {
    @QueryParams(name = "name")
    private String name;
}
