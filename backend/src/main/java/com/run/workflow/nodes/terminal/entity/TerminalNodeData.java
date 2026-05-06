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
     * 代码来源: reference 或 customize
     */
    private String location;
    /**
     * 代码引用变量路径
     */
    private List<String> reference;
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
