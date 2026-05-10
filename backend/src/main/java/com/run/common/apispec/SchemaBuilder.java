package com.run.common.apispec;

import io.swagger.v3.oas.models.media.*;

import java.math.BigDecimal;
import java.util.*;

public class SchemaBuilder {

    private Schema<?> schema;
    private final Map<String, String> messages = new LinkedHashMap<>();
    private String requiredMessage;

    private SchemaBuilder(Schema<?> schema) {
        this.schema = schema;
    }

    public static SchemaBuilder string() {
        return new SchemaBuilder(new StringSchema());
    }

    public static SchemaBuilder integer() {
        return new SchemaBuilder(new IntegerSchema());
    }

    public static SchemaBuilder number() {
        return new SchemaBuilder(new NumberSchema());
    }

    public static SchemaBuilder bool() {
        return new SchemaBuilder(new BooleanSchema());
    }

    public static SchemaBuilder array(Schema<?> items) {
        return new SchemaBuilder(new ArraySchema().items(items));
    }

    public static SchemaBuilder object() {
        return new SchemaBuilder(new JsonSchema());
    }

    public static SchemaBuilder ref(String ref) {
        return new SchemaBuilder(new Schema<>().$ref(ref));
    }

    public static FileSchemaBuilder file() {
        return new FileSchemaBuilder();
    }

    // String validations

    public SchemaBuilder minLength(int value, String message) {
        schema.setMinLength(value);
        if (message != null) messages.put("minLength", message);
        return this;
    }

    public SchemaBuilder maxLength(int value, String message) {
        schema.setMaxLength(value);
        if (message != null) messages.put("maxLength", message);
        return this;
    }

    public SchemaBuilder pattern(String value, String message) {
        schema.setPattern(value);
        if (message != null) messages.put("pattern", message);
        return this;
    }

    // Number validations

    public SchemaBuilder minimum(Number value, String message) {
        schema.setMinimum(BigDecimal.valueOf(value.doubleValue()));
        if (message != null) messages.put("minimum", message);
        return this;
    }

    public SchemaBuilder maximum(Number value, String message) {
        schema.setMaximum(BigDecimal.valueOf(value.doubleValue()));
        if (message != null) messages.put("maximum", message);
        return this;
    }

    // Common

    public SchemaBuilder description(String desc) {
        schema.setDescription(desc);
        return this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public SchemaBuilder enumValues(List<?> values) {
        ((Schema) schema).setEnum(values);
        return this;
    }

    public SchemaBuilder requiredMessage(String message) {
        this.requiredMessage = message;
        return this;
    }

    // Object-specific: build with required + properties

    public Schema<?> build(List<String> required, LinkedHashMap<String, Schema<?>> properties) {
        if (required != null && !required.isEmpty()) {
            schema.setRequired(required);
        }
        if (properties != null) {
            schema.setProperties(new LinkedHashMap<>(properties));
        }
        applyMessages();
        return schema;
    }

    // Leaf build (non-object)

    public Schema<?> build() {
        applyMessages();
        return schema;
    }

    private void applyMessages() {
        if (!messages.isEmpty()) {
            schema.addExtension("x-messages", messages);
        }
        if (requiredMessage != null) {
            schema.addExtension("x-required-message", requiredMessage);
        }
    }

    // ===== Convenience: Object builder with fluent API =====

    public static ObjectSchemaBuilder objectBuilder() {
        return new ObjectSchemaBuilder();
    }

    // ===== File schema builder =====

    public static class FileSchemaBuilder {
        private String description;
        private String requiredMessage;
        private Long maxSize;
        private String maxSizeMessage;
        private Long minSize;
        private String minSizeMessage;
        private List<String> allowedTypes;
        private String allowedTypesMessage;
        private Integer maxCount;
        private String maxCountMessage;

        public FileSchemaBuilder description(String desc) {
            this.description = desc;
            return this;
        }

        public FileSchemaBuilder required(String message) {
            this.requiredMessage = message;
            return this;
        }

        public FileSchemaBuilder maxSize(long bytes, String message) {
            this.maxSize = bytes;
            this.maxSizeMessage = message;
            return this;
        }

        public FileSchemaBuilder minSize(long bytes, String message) {
            this.minSize = bytes;
            this.minSizeMessage = message;
            return this;
        }

        public FileSchemaBuilder allowedTypes(List<String> types, String message) {
            this.allowedTypes = types;
            this.allowedTypesMessage = message;
            return this;
        }

        public FileSchemaBuilder maxCount(int count, String message) {
            this.maxCount = count;
            this.maxCountMessage = message;
            return this;
        }

        public Schema<?> build() {
            FileSchema s = new FileSchema();
            if (description != null) s.setDescription(description);
            if (maxSize != null) s.addExtension("x-max-size", maxSize);
            if (maxSizeMessage != null) s.addExtension("x-max-size-message", maxSizeMessage);
            if (minSize != null) s.addExtension("x-min-size", minSize);
            if (minSizeMessage != null) s.addExtension("x-min-size-message", minSizeMessage);
            if (allowedTypes != null) s.addExtension("x-allowed-types", allowedTypes);
            if (allowedTypesMessage != null) s.addExtension("x-allowed-types-message", allowedTypesMessage);
            if (maxCount != null) s.addExtension("x-max-count", maxCount);
            if (maxCountMessage != null) s.addExtension("x-max-count-message", maxCountMessage);
            if (requiredMessage != null) s.addExtension("x-required-message", requiredMessage);
            return s;
        }
    }

    public static class ObjectSchemaBuilder {
        private final List<String> required = new ArrayList<>();
        private final LinkedHashMap<String, Schema<?>> properties = new LinkedHashMap<>();
        private final Map<String, String> requiredMessages = new LinkedHashMap<>();

        public ObjectSchemaBuilder required(String field, Schema<?> schema, String message) {
            required.add(field);
            properties.put(field, schema);
            if (message != null) {
                requiredMessages.put(field, message);
            }
            return this;
        }

        public ObjectSchemaBuilder required(String field, Schema<?> schema) {
            return required(field, schema, null);
        }

        public ObjectSchemaBuilder property(String field, Schema<?> schema) {
            properties.put(field, schema);
            return this;
        }

        public Schema<?> build() {
            JsonSchema s = new JsonSchema();
            if (!required.isEmpty()) {
                s.setRequired(required);
            }
            s.setProperties(new LinkedHashMap<>(properties));
            if (!requiredMessages.isEmpty()) {
                s.addExtension("x-required-messages", requiredMessages);
            }
            return s;
        }
    }
}
