package com.run.common.query.annotations;

import com.run.common.query.constants.LocationConstants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/22  21:34}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface QueryParams {
    /**
     * 查询字段
     *
     * @return 查询字段
     */
    String name() default "";

    /**
     * query | path
     *
     * @return 参数位置
     */
    LocationConstants location() default LocationConstants.QUERY;
}
