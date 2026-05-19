package com.run.workflow.deserialize.filedownload;

import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class FileDownloadDeserialize implements INodeDeserialize {

    @Override
    public boolean support(String type) {
        return "file-download-node".equals(type);
    }

    @Override
    public Map<String, Object> deserialize(JsonObject context) {
        Map<String, Object> result = new HashMap<>();
        if (context.containsKey("filePath")) result.put("filePath", context.getString("filePath"));
        if (context.containsKey("fileName")) result.put("fileName", context.getString("fileName"));
        if (context.containsKey("fileSize")) result.put("fileSize", context.getLong("fileSize"));
        if (context.containsKey("tool")) result.put("tool", context.getJsonObject("tool"));
        return result;
    }
}
