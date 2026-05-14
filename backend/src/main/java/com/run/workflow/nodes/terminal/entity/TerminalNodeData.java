package com.run.workflow.nodes.terminal.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 终端执行节点数据
 */
@Getter
@Setter
public class TerminalNodeData {
    /**
     * 顶层模式: tool_call 或 customize
     */
    private String location;
    /**
     * tool_call 引用变量路径
     */
    private List<String> reference;
    /**
     * 代码子模式: reference 或 customize
     */
    private String codeLocation;
    /**
     * 代码引用变量路径
     */
    private List<String> codeReference;
    /**
     * 自定义代码
     */
    private String code;

    /**
     * 超时来源: reference 或 customize
     */
    private String timeoutLocation;
    /**
     * 超时引用变量路径
     */
    private List<String> timeoutReference;
    /**
     * 自定义超时时间（秒）
     */
    private Integer timeout;
}
