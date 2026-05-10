package com.run.common.apispec;

import io.swagger.v3.oas.models.media.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SchemaValidator {

    public static void validate(String source, Schema<?> schema, JsonObject data) {
        if (schema == null || data == null) return;
        List<ValidationException.ValidationError> errors = new ArrayList<>();
        validateObject(source, "", schema, data, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    public static List<ValidationException.ValidationError> validateQuietly(String source, Schema<?> schema, JsonObject data) {
        if (schema == null || data == null) return List.of();
        List<ValidationException.ValidationError> errors = new ArrayList<>();
        validateObject(source, "", schema, data, errors);
        return errors;
    }

    /**
     * 校验文件上传
     *
     * @param fieldName 表单字段名
     * @param schema    文件 Schema（由 SchemaBuilder.file() 构建）
     * @param uploads   所有文件上传列表
     */
    public static void validateFile(String fieldName, Schema<?> schema, List<FileUpload> uploads) {
        if (schema == null) return;
        List<ValidationException.ValidationError> errors = new ArrayList<>();
        validateFileQuietly(fieldName, schema, uploads, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    public static void validateFileQuietly(String fieldName, Schema<?> schema, List<FileUpload> uploads,
                                           List<ValidationException.ValidationError> errors) {
        if (schema == null) return;
        Map<String, Object> ext = schema.getExtensions();
        if (ext == null) ext = Map.of();

        List<FileUpload> matched = uploads.stream()
                .filter(u -> fieldName.equals(u.name()))
                .collect(Collectors.toList());

        // Required
        String requiredMsg = (String) ext.get("x-required-message");
        if (requiredMsg != null && matched.isEmpty()) {
            errors.add(new ValidationException.ValidationError("file", fieldName, requiredMsg));
            return;
        }
        if (matched.isEmpty()) return;

        // Max count
        Integer maxCount = (Integer) ext.get("x-max-count");
        String maxCountMsg = (String) ext.get("x-max-count-message");
        if (maxCount != null && matched.size() > maxCount) {
            errors.add(new ValidationException.ValidationError("file", fieldName,
                    maxCountMsg != null ? maxCountMsg : fieldName + " 最多上传 " + maxCount + " 个文件"));
        }

        for (FileUpload upload : matched) {
            // File size
            Long maxSize = (Long) ext.get("x-max-size");
            String maxSizeMsg = (String) ext.get("x-max-size-message");
            if (maxSize != null && upload.size() > maxSize) {
                errors.add(new ValidationException.ValidationError("file", fieldName,
                        maxSizeMsg != null ? maxSizeMsg : fieldName + " 大小不能超过 " + formatSize(maxSize)));
            }

            Long minSize = (Long) ext.get("x-min-size");
            String minSizeMsg = (String) ext.get("x-min-size-message");
            if (minSize != null && upload.size() < minSize) {
                errors.add(new ValidationException.ValidationError("file", fieldName,
                        minSizeMsg != null ? minSizeMsg : fieldName + " 大小不能小于 " + formatSize(minSize)));
            }

            // Content type
            @SuppressWarnings("unchecked")
            List<String> allowedTypes = (List<String>) ext.get("x-allowed-types");
            String allowedTypesMsg = (String) ext.get("x-allowed-types-message");
            if (allowedTypes != null && !allowedTypes.isEmpty()) {
                String contentType = upload.contentType();
                boolean matched2 = allowedTypes.stream().anyMatch(t ->
                        t.equalsIgnoreCase(contentType) || contentType.endsWith("/" + t));
                if (!matched2) {
                    errors.add(new ValidationException.ValidationError("file", fieldName,
                            allowedTypesMsg != null ? allowedTypesMsg
                                    : fieldName + " 只支持 " + String.join("/", allowedTypes) + " 格式"));
                }
            }
        }
    }

    private static String formatSize(long bytes) {
        if (bytes >= 1024 * 1024) return (bytes / 1024 / 1024) + "MB";
        if (bytes >= 1024) return (bytes / 1024) + "KB";
        return bytes + "B";
    }

    @SuppressWarnings("unchecked")
    private static void validateObject(String source, String path, Schema<?> schema, JsonObject data,
                                       List<ValidationException.ValidationError> errors) {
        // Validate required fields
        List<String> required = schema.getRequired();
        if (required != null) {
            Map<String, String> requiredMessages = getRequiredMessages(schema);
            for (String field : required) {
                String fullPath = joinPath(path, field);
                if (!data.containsKey(field) || data.getValue(field) == null) {
                    String msg = requiredMessages != null ? requiredMessages.get(field) : null;
                    errors.add(new ValidationException.ValidationError(source, fullPath,
                            msg != null ? msg : fullPath + " 为必填项"));
                } else {
                    Object value = data.getValue(field);
                    if (value instanceof String s && s.isBlank()) {
                        String msg = requiredMessages != null ? requiredMessages.get(field) : null;
                        errors.add(new ValidationException.ValidationError(source, fullPath,
                                msg != null ? msg : fullPath + " 不能为空"));
                    }
                }
            }
        }

        // Validate each property
        Map<String, Schema> properties = schema.getProperties();
        if (properties == null) return;

        for (Map.Entry<String, Schema> entry : properties.entrySet()) {
            String field = entry.getKey();
            Schema<?> propSchema = entry.getValue();
            String fullPath = joinPath(path, field);

            if (!data.containsKey(field)) continue;
            Object value = data.getValue(field);
            if (value == null) continue;

            Map<String, String> messages = getMessages(propSchema);
            validateValue(source, fullPath, propSchema, value, messages, errors);
        }
    }

    private static void validateValue(String source, String fullPath, Schema<?> schema, Object value,
                                      Map<String, String> messages,
                                      List<ValidationException.ValidationError> errors) {
        if (schema instanceof StringSchema) {
            validateString(source, fullPath, schema, value, messages, errors);
        } else if (schema instanceof IntegerSchema) {
            validateInteger(source, fullPath, schema, value, messages, errors);
        } else if (schema instanceof NumberSchema) {
            validateNumber(source, fullPath, schema, value, messages, errors);
        } else if (schema instanceof BooleanSchema) {
            if (!(value instanceof Boolean)) {
                errors.add(new ValidationException.ValidationError(source, fullPath,
                        message(messages, "type", fullPath + " 必须为布尔类型")));
            }
        } else if (schema instanceof ArraySchema arraySchema) {
            validateArray(source, fullPath, arraySchema, value, messages, errors);
        } else if (schema instanceof JsonSchema) {
            if (value instanceof JsonObject obj) {
                validateObject(source, fullPath, schema, obj, errors);
            } else {
                errors.add(new ValidationException.ValidationError(source, fullPath,
                        message(messages, "type", fullPath + " 必须为对象类型")));
            }
        }
    }

    private static void validateString(String source, String fullPath, Schema<?> schema, Object value,
                                       Map<String, String> messages,
                                       List<ValidationException.ValidationError> errors) {
        if (!(value instanceof String str)) {
            errors.add(new ValidationException.ValidationError(source, fullPath,
                    message(messages, "type", fullPath + " 必须为字符串类型")));
            return;
        }
        if (schema.getMinLength() != null && str.length() < schema.getMinLength().intValue()) {
            errors.add(new ValidationException.ValidationError(source, fullPath,
                    message(messages, "minLength", fullPath + " 长度不能少于 " + schema.getMinLength())));
        }
        if (schema.getMaxLength() != null && str.length() > schema.getMaxLength().intValue()) {
            errors.add(new ValidationException.ValidationError(source, fullPath,
                    message(messages, "maxLength", fullPath + " 长度不能超过 " + schema.getMaxLength())));
        }
        if (schema.getPattern() != null && !str.matches(schema.getPattern())) {
            errors.add(new ValidationException.ValidationError(source, fullPath,
                    message(messages, "pattern", fullPath + " 格式不正确")));
        }
        if (schema.getEnum() != null && !schema.getEnum().contains(str)) {
            errors.add(new ValidationException.ValidationError(source, fullPath,
                    message(messages, "enum", fullPath + " 值不在允许范围内")));
        }
    }

    private static void validateInteger(String source, String fullPath, Schema<?> schema, Object value,
                                        Map<String, String> messages,
                                        List<ValidationException.ValidationError> errors) {
        Number num = toNumber(value);
        if (num == null) {
            errors.add(new ValidationException.ValidationError(source, fullPath,
                    message(messages, "type", fullPath + " 必须为整数类型")));
            return;
        }
        if (num.doubleValue() != Math.floor(num.doubleValue())) {
            errors.add(new ValidationException.ValidationError(source, fullPath,
                    message(messages, "type", fullPath + " 必须为整数类型")));
            return;
        }
        validateNumberRange(source, fullPath, schema, num, messages, errors);
    }

    private static void validateNumber(String source, String fullPath, Schema<?> schema, Object value,
                                       Map<String, String> messages,
                                       List<ValidationException.ValidationError> errors) {
        Number num = toNumber(value);
        if (num == null) {
            errors.add(new ValidationException.ValidationError(source, fullPath,
                    message(messages, "type", fullPath + " 必须为数字类型")));
            return;
        }
        validateNumberRange(source, fullPath, schema, num, messages, errors);
    }

    private static void validateNumberRange(String source, String fullPath, Schema<?> schema, Number num,
                                            Map<String, String> messages,
                                            List<ValidationException.ValidationError> errors) {
        if (schema.getMinimum() != null && num.doubleValue() < schema.getMinimum().doubleValue()) {
            errors.add(new ValidationException.ValidationError(source, fullPath,
                    message(messages, "minimum", fullPath + " 不能小于 " + schema.getMinimum())));
        }
        if (schema.getMaximum() != null && num.doubleValue() > schema.getMaximum().doubleValue()) {
            errors.add(new ValidationException.ValidationError(source, fullPath,
                    message(messages, "maximum", fullPath + " 不能大于 " + schema.getMaximum())));
        }
    }

    private static void validateArray(String source, String fullPath, ArraySchema schema, Object value,
                                      Map<String, String> messages,
                                      List<ValidationException.ValidationError> errors) {
        if (!(value instanceof JsonArray arr)) {
            errors.add(new ValidationException.ValidationError(source, fullPath,
                    message(messages, "type", fullPath + " 必须为数组类型")));
            return;
        }
        Schema<?> items = schema.getItems();
        if (items == null) return;
        for (int i = 0; i < arr.size(); i++) {
            String itemPath = fullPath + "[" + i + "]";
            Object item = arr.getValue(i);
            if (item instanceof JsonObject obj) {
                if (items instanceof JsonSchema) {
                    validateObject(source, itemPath, items, obj, errors);
                }
            } else if (item != null) {
                validateValue(source, itemPath, items, item, getMessages(items), errors);
            }
        }
    }

    private static Number toNumber(Object value) {
        if (value instanceof Number n) return n;
        if (value instanceof String s) {
            try {
                if (s.contains(".")) return Double.parseDouble(s);
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> getMessages(Schema<?> schema) {
        if (schema == null || schema.getExtensions() == null) return null;
        Object ext = schema.getExtensions().get("x-messages");
        if (ext instanceof Map) return (Map<String, String>) ext;
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> getRequiredMessages(Schema<?> schema) {
        if (schema == null || schema.getExtensions() == null) return null;
        Object ext = schema.getExtensions().get("x-required-messages");
        if (ext instanceof Map) return (Map<String, String>) ext;
        return null;
    }

    private static String message(Map<String, String> messages, String key, String defaultMsg) {
        if (messages != null && messages.containsKey(key)) return messages.get(key);
        return defaultMsg;
    }

    private static String joinPath(String parent, String child) {
        if (parent == null || parent.isEmpty()) return child;
        return parent + "." + child;
    }
}
