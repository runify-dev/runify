package com.run.workflow.nodes.runtool.entity;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RunToolNodeData {
    /**
     * 引用的工具 id
     */
    private String toolId;
    /**
     * 入参映射(绑定上游变量/常量到 inputSchema 字段)
     */
    private List<InputBinding> inputs;
    /**
     * 配置覆盖值(绑定层，覆盖工具默认 config)
     */
    private Map<String, Object> config;

    @Data
    public static class InputBinding {
        private String field;
        /**
         * reference / customize
         */
        private String location;
        /**
         * reference=变量路径 List<String>；customize=常量
         */
        private Object value;
    }
}
