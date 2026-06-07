package com.run.workflow.deserialize.downloadskills;

import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class DownloadSkillsDeserialize implements INodeDeserialize {

    @Override
    public boolean support(String type) {
        return "download-skills-node".equals(type);
    }

    @Override
    public Map<String, Object> deserialize(JsonObject context) {
        Map<String, Object> result = new HashMap<>();
        if (context.containsKey("skillId")) result.put("skillId", context.getString("skillId"));
        if (context.containsKey("skillName")) result.put("skillName", context.getString("skillName"));
        if (context.containsKey("files")) result.put("files", context.getInteger("files"));
        if (context.containsKey("status")) result.put("status", context.getString("status"));
        if (context.containsKey("tool")) result.put("tool", context.getJsonObject("tool"));
        return result;
    }
}
