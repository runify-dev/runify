package com.run.handler.conversation.vo;

import com.run.common.query.annotations.QueryParams;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class QueryConversationMessageVO {
    /**
     * 当前页
     */
    @QueryParams(name = "currentPage")
    @NotNull(message = "当前页不能为空")
    private Long currentPage;
    /**
     * 每页大小
     */
    @QueryParams(name = "pageSize")
    @NotNull(message = "每页大小不能为空")
    private Long pageSize;
}
