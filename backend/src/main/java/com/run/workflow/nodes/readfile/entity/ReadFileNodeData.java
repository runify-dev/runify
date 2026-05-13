package com.run.workflow.nodes.readfile.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReadFileNodeData {
    /**
     * 模式: tool_call / customize
     */
    private String location;

    // ── tool_call 模式 ──

    /**
     * tool_call 引用变量路径
     */
    private List<String> reference;

    // ── customize 模式 ──

    /**
     * 文件路径来源: reference / customize
     */
    private String pathLocation;
    private List<String> pathReference;
    private String path;

    /**
     * 偏移行号来源: reference / customize
     */
    private String offsetLocation;
    private List<String> offsetReference;
    private Integer offset;

    /**
     * 读取行数来源: reference / customize
     */
    private String limitLocation;
    private List<String> limitReference;
    private Integer limit;
}
