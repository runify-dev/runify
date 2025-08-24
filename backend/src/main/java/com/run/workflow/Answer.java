package com.run.workflow;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/8/23  18:18}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Answer extends JsonObject {
    /**
     * 节点执行的唯一id
     */
    private String realNodeId;
    /**
     * 节点id
     */
    private String nodeId;
    /**
     * 显示id
     */
    private String displayId;
    /**
     * 对话记录id
     */
    private String conversationRecordId;
    /**
     * 对话id
     */
    private String conversationId;


    @JsonAnySetter
    @Override
    public JsonObject put(String key, Object value) {
        return super.put(key, value);
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("real_node_id", this.realNodeId);
        result.put("node_id", this.nodeId);
        result.put("display_id", this.displayId);
        result.put("conversation_id", this.conversationId);
        result.put("conversation_record_id", this.conversationRecordId);
        return result;
    }

}
