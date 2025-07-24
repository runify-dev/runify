package com.run.workflow;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/30  15:13}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class Tools {

    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
