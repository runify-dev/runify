package com.run.common.util;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/21  22:48}
 * {@code @Version 1.0}
 * {@code @注释: }
 */

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Jackson JSON 工具类
 */
public class JacksonUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        // 初始化配置
        OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        OBJECT_MAPPER.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        OBJECT_MAPPER.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        OBJECT_MAPPER.configure(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, true);
        OBJECT_MAPPER.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        // 注册 Java 8 时间模块
        OBJECT_MAPPER.registerModule(new JavaTimeModule());

        // Long 类型转为 String 处理（避免前端精度丢失）
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        OBJECT_MAPPER.registerModule(simpleModule);

// Long 转 String
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);

// Vert.x JsonObject 序列化
        simpleModule.addSerializer(JsonObject.class, new JsonSerializer<JsonObject>() {
            @Override
            public void serialize(JsonObject value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value == null) {
                    gen.writeNull();
                    return;
                }
                gen.writeObject(value.getMap());
            }
        });

// Vert.x JsonArray 序列化
        simpleModule.addSerializer(JsonArray.class, new JsonSerializer<JsonArray>() {
            @Override
            public void serialize(JsonArray value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value == null) {
                    gen.writeNull();
                    return;
                }
                gen.writeObject(value.getList());
            }
        });

// Vert.x JsonObject 反序列化
        simpleModule.addDeserializer(JsonObject.class, new JsonDeserializer<JsonObject>() {
            @Override
            public JsonObject deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                Map<String, Object> map = p.getCodec().readValue(p, new TypeReference<Map<String, Object>>() {
                });
                return new JsonObject(map);
            }
        });

// Vert.x JsonArray 反序列化
        simpleModule.addDeserializer(JsonArray.class, new JsonDeserializer<JsonArray>() {
            @Override
            public JsonArray deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                List<Object> list = p.getCodec().readValue(p, new TypeReference<List<Object>>() {
                });
                return new JsonArray(list);
            }
        });

        OBJECT_MAPPER.registerModule(simpleModule);
    }

    /**
     * 对象转 JSON 字符串
     */
    public static String toJson(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("对象转JSON字符串失败", e);
        }
    }

    /**
     * 对象转 JSON 字符串（美化输出）
     */
    public static String toJsonPretty(Object obj) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("对象转JSON字符串(美化)失败", e);
        }
    }

    /**
     * JSON 字符串转对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            if (clazz == JsonObject.class) {
                return clazz.cast(new JsonObject(json));
            }

            if (clazz == JsonArray.class) {
                return clazz.cast(new JsonArray(json));
            }
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException("JSON字符串转对象失败", e);
        }
    }

    /**
     * JSON 字符串转复杂对象（如List、Map等）
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (IOException e) {
            throw new RuntimeException("JSON字符串转复杂对象失败", e);
        }
    }

    /**
     * JSON 字符串转 JsonNode
     */
    public static JsonNode readTree(String json) {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (IOException e) {
            throw new RuntimeException("JSON字符串转JsonNode失败", e);
        }
    }

    /**
     * 对象转换（深拷贝）
     */
    public static <T> T convert(Object fromValue, Class<T> toValueType) {
        return OBJECT_MAPPER.convertValue(fromValue, toValueType);
    }

    /**
     * 对象转换（复杂类型深拷贝）
     */
    public static <T> T convert(Object fromValue, TypeReference<T> toValueTypeRef) {
        return OBJECT_MAPPER.convertValue(fromValue, toValueTypeRef);
    }

    /**
     * 获取 ObjectMapper 实例
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }
}