package com.run.workflow.nodes.apppatch.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ApplyPatchNodeData {
    /**
     * 顶层模式: tool_call 或 customize
     */
    private String location;
    /**
     * tool_call 引用变量路径
     */
    private List<String> reference;
    /**
     * patch 子模式: reference 或 customize
     */
    private String patchLocation;
    /**
     * patch 引用变量路径
     */
    private List<String> patchReference;
    /**
     * 自定义 patch 字符串
     */
    private String patch;
    /**
     * 工作目录（patch 中相对路径的基准目录）
     */
    private String path;
    /**
     * 工作目录子模式: reference 或 customize
     */
    private String pathLocation;
    /**
     * 工作目录引用变量路径
     */
    private List<String> pathReference;
}
