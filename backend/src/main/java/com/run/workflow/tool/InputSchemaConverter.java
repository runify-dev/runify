package com.run.workflow.tool;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * 把 dynamics-form-plus 的 FormField[] (inputSchema) 转成 JSON Schema。
 * 供工具作为 LLM function 时生成 {@code function.parameters}。
 * <p>
 * 仅 inputSchema 参与转换；configSchema/outputSchema 不对 LLM 暴露。
 */
public class InputSchemaConverter {

    private InputSchemaConverter() {
    }

    public static JsonObject toJsonSchema(JsonArray inputSchema) {
        JsonObject properties = new JsonObject();
        JsonArray required = new JsonArray();
        if (inputSchema != null) {
            for (int i = 0; i < inputSchema.size(); i++) {
                JsonObject field = inputSchema.getJsonObject(i);
                String name = field.getString("field");
                if (name == null || name.isEmpty()) continue;
                properties.put(name, fieldToProperty(field));
                if (Boolean.TRUE.equals(field.getBoolean("required"))) {
                    required.add(name);
                }
            }
        }
        JsonObject schema = new JsonObject()
                .put("type", "object")
                .put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    private static JsonObject fieldToProperty(JsonObject field) {
        String type = field.getString("type", "TextInput");
        JsonObject property = new JsonObject();
        switch (type) {
            case "Slider" -> property.put("type", "number");
            case "SwitchInput" -> property.put("type", "boolean");
            case "MultiSelect" -> property.put("type", "array").put("items", new JsonObject().put("type", "string"));
            case "JsonInput" -> property.put("type", "object");
            default -> property.put("type", "string");
        }
        String description = resolveDescription(field);
        if (description != null && !description.isEmpty()) {
            property.put("description", description);
        }
        // 枚举
        JsonArray optionList = field.getJsonArray("optionList");
        if (optionList != null && !optionList.isEmpty()) {
            String valueField = field.getString("valueField", "value");
            JsonArray enums = new JsonArray();
            for (int i = 0; i < optionList.size(); i++) {
                Object opt = optionList.getValue(i);
                if (opt instanceof JsonObject jo) {
                    enums.add(jo.getValue(valueField));
                } else {
                    enums.add(opt);
                }
            }
            if ("MultiSelect".equals(type)) {
                property.put("items", new JsonObject().put("type", "string").put("enum", enums));
            } else {
                property.put("enum", enums);
            }
        }
        return property;
    }

    /**
     * label 可能是字符串或 {value, tooltip} 对象；attrs.description 优先
     */
    private static String resolveDescription(JsonObject field) {
        JsonObject attrs = field.getJsonObject("attrs");
        if (attrs != null && attrs.getString("description") != null) {
            return attrs.getString("description");
        }
        Object label = field.getValue("label");
        if (label instanceof JsonObject lo) {
            return lo.getString("tooltip", lo.getString("value"));
        }
        if (label instanceof String s) {
            return s;
        }
        return null;
    }
}
