package com.run.workflow.deserialize.fileupload;

import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class FileUploadDeserialize implements INodeDeserialize {

    @Override
    public boolean support(String type) {
        return "file-upload-node".equals(type);
    }

    @Override
    public Map<String, Object> deserialize(JsonObject context) {
        Map<String, Object> result = new HashMap<>();
        if (context.containsKey("fileId")) {
            result.put("fileId", context.getString("fileId"));
        }
        if (context.containsKey("fileName")) {
            result.put("fileName", context.getString("fileName"));
        }
        if (context.containsKey("fileSize")) {
            result.put("fileSize", context.getValue("fileSize"));
        }
        return result;
    }
}
