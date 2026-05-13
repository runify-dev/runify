package com.run.workflow.nodes.listdir.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ListDirNodeData {
    /**
     * 模式: tool_call / customize
     */
    private String location;

    // ── tool_call 模式 ──
    private List<String> reference;

    // ── customize 模式 ──
    private String pathLocation;
    private List<String> pathReference;
    private String path;

    private String depthLocation;
    private List<String> depthReference;
    private Integer depth;
}
