package com.run.workflow.deserialize.start;

import com.run.dao.entity.ConversationMessage;
import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.Strings;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

public class StartNodeDeserialize implements INodeDeserialize {
    private final static HashMap<String, BiFunction<String, JsonObject, Object>> deserializeMap = new HashMap<>();

    static {
        deserializeMap.put("question", (key, c) -> c.getString(key));
        deserializeMap.put("messages", (key, c) -> {
            JsonArray messages = c.getJsonArray("messages");
            return IntStream.range(0, messages.size()).boxed().map(index -> messages.getJsonObject(index).mapTo(ConversationMessage.class)).toList();
        });
    }

    @Override
    public boolean support(String type) {
        return Strings.CS.equals(type, "start-node");
    }

    @Override
    public Map<String, Object> deserialize(JsonObject context) {
        Set<String> keys = context.fieldNames();
        Map<String, Object> result = new HashMap<>();
        for (String key : keys) {
            result.put(key, deserializeMap.get(key).apply(key, context));
        }
        return result;
    }


}
