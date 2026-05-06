package com.run.workflow.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/21  23:18}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NodeField {
    /**
     * 节点id
     */
    private String nodeId;
    /**
     * 节点名称
     */
    private String nodeName;
    /**
     * label
     */
    private String label;
    /**
     * 值
     */
    private String value;

    public String resetVariable(String prompt) {
        String userVariable = this.nodeName + "." + this.value;
        String systemVariable = String.format("(context['%s']['%s'])!\"\"", this.nodeId, this.value);
        return prompt.replaceAll(userVariable, systemVariable);
    }

}
