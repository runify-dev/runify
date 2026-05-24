package com.run.handler.application.vo;

import com.run.dao.common.annotations.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OverviewStatsVO {
    @Column(name = "conversation_count")
    private Long conversationCount;

    @Column(name = "message_count")
    private Long messageCount;

    @Column(name = "total_tokens")
    private Long totalTokens;

    @Column(name = "avg_duration")
    private Long avgDuration;
}
