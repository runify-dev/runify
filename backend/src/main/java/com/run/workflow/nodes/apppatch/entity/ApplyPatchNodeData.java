package com.run.workflow.nodes.apppatch.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ApplyPatchNodeData {
    /**
     * patch 来源: reference 或 customize
     */
    private String location;
    /**
     * patch 引用变量路径
     */
    private List<String> reference;
    /**
     * 自定义 patch 字符串
     */
    private String patch;
}
