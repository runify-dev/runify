package com.run.workflow.message.struct;

import com.run.common.constants.ContentTypeConstants;
import com.run.workflow.INode;
import com.run.workflow.NodeStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/2/5  22:24}
 * {@code @Version 1.0}
 * {@code @注释:  用于连续写出  也就是stream流式写出 每个节点都可以输出他产生的数据 比如第一个节点产生name 第二个节点产生age}
 */
@Getter
@Setter
@NoArgsConstructor
public class JsonFieldsContent extends AnswerContent {
    private Map<String, Object> content;

    public JsonFieldsContent(Map<String, Object> content, INode<?, ?> node, String workflowRunId, String id) {
        super(ContentTypeConstants.JSON_FIELD, node, workflowRunId, id);
        this.content = content;
    }

    public JsonFieldsContent(Map<String, Object> content, String workflowRunId,
                             String id,
                             NodeStatus status,
                             String nodeId,
                             String nodeName) {
        super(ContentTypeConstants.JSON_FIELD, workflowRunId, id, nodeId, status, nodeName);
        this.content = content;
    }

    public JsonFieldsContent(Map<String, Object> content, String workflowRunId, String id, Map<String, Object> extra) {
        super(ContentTypeConstants.JSON_FIELD, workflowRunId, id, extra);
        this.content = content;
    }

    @Override
    public String toString() {
        return content.toString();
    }
}
