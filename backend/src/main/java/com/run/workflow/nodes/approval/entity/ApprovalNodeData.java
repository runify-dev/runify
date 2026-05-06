package com.run.workflow.nodes.approval.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ApprovalNodeData {
    /**
     * 提示来源: reference 或 customize
     */
    private String location;
    /**
     * 引用变量路径
     */
    private List<String> reference;
    /**
     * 自定义提示内容
     */
    private String prompt;
}
