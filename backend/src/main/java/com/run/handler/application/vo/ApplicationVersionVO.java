package com.run.handler.application.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/7/28}
 * {@code @Version 1.0}
 * {@code @注释: 发布历史列表项(不含 snapshot,避免列表接口传输大对象)}
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ApplicationVersionVO {
    private UUID id;
    private Integer version;
    private String remark;
    private UUID createUser;
    private String createUserName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
