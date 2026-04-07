package com.run.workflow.converter;

import com.run.common.constants.ContentTypeConstants;
import io.vertx.core.json.JsonObject;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/6  17:40}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ContentConvertConfig {

    private final Map<ContentTypeConstants, ContentConvertStrategy> strategies;
    private final Map<ContentTypeConstants, String> prefixes;
    private final Map<ContentTypeConstants, Function<JsonObject, String>> extractors;

    private ContentConvertConfig(Builder builder) {
        this.strategies = Collections.unmodifiableMap(new EnumMap<>(builder.strategies));
        this.prefixes = Collections.unmodifiableMap(new EnumMap<>(builder.prefixes));
        this.extractors = Collections.unmodifiableMap(new EnumMap<>(builder.extractors));
    }

    public ContentConvertStrategy getStrategy(ContentTypeConstants type) {
        return strategies.getOrDefault(type, ContentConvertStrategy.IGNORE);
    }

    public String getPrefix(ContentTypeConstants type) {
        return prefixes.getOrDefault(type, "");
    }

    public String extract(ContentTypeConstants type, JsonObject obj) {
        return extractors
                .getOrDefault(type, o -> o.getString("content", ""))
                .apply(obj);
    }

    public static ContentConvertConfig defaultConfig() {
        return builder()
                .text(ContentTypeConstants.TEXT,
                        obj -> obj.getString("content", ""))

                .text(ContentTypeConstants.SYSTEM,
                        obj -> obj.getString("content", ""))

                .text(ContentTypeConstants.QUESTION,
                        obj -> obj.getString("content", ""))

                .ignore(ContentTypeConstants.REASONING)

                .ignore(ContentTypeConstants.TOOL)

                .prefixed(ContentTypeConstants.FAILURE, "[Error]\n",
                        obj -> obj.getString("content", ""))

                .prefixed(ContentTypeConstants.HEADERS, "[HTTP Headers]\n",
                        obj -> {
                            JsonObject headers = obj.getJsonObject("content");
                            if (headers == null) return "";
                            return headers.stream()
                                    .map(e -> e.getKey() + ": " + e.getValue())
                                    .collect(Collectors.joining("\n"));
                        })

                .prefixed(ContentTypeConstants.JSON, "[Json]\n",
                        obj -> obj.getString("content", ""))

                .prefixed(ContentTypeConstants.JSON_FIELD, "[Json]\n",
                        obj -> {
                            JsonObject fields = obj.getJsonObject("content");
                            return fields != null ? fields.toString() : "";
                        })

                .prefixed(ContentTypeConstants.STATUS, "[Status]\n",
                        obj -> {
                            Integer status = obj.getInteger("content");
                            return status != null ? status.toString() : "";
                        })

                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<ContentTypeConstants, ContentConvertStrategy> strategies = new EnumMap<>(ContentTypeConstants.class);
        private final Map<ContentTypeConstants, String> prefixes = new EnumMap<>(ContentTypeConstants.class);
        private final Map<ContentTypeConstants, Function<JsonObject, String>> extractors = new EnumMap<>(ContentTypeConstants.class);

        public Builder ignore(ContentTypeConstants type) {
            strategies.put(type, ContentConvertStrategy.IGNORE);
            return this;
        }

        public Builder text(ContentTypeConstants type, Function<JsonObject, String> extractor) {
            strategies.put(type, ContentConvertStrategy.TEXT);
            extractors.put(type, extractor);
            return this;
        }

        public Builder prefixed(ContentTypeConstants type, String prefix, Function<JsonObject, String> extractor) {
            strategies.put(type, ContentConvertStrategy.PREFIXED);
            prefixes.put(type, prefix);
            extractors.put(type, extractor);
            return this;
        }

        public Builder prefix(ContentTypeConstants type, String prefix) {
            prefixes.put(type, prefix);
            return this;
        }

        public Builder extractor(ContentTypeConstants type, Function<JsonObject, String> extractor) {
            extractors.put(type, extractor);
            return this;
        }

        public ContentConvertConfig build() {
            return new ContentConvertConfig(this);
        }
    }
}