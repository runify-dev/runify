package com.run.dao.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/11  22:58}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface Column {
    /**
     * 字段名称
     *
     * @return 对应的数据库字段名称
     */
    String name() default "";

    /**
     * 是否为主键
     *
     * @return 是否为主键
     */
    boolean primaryKey() default false;
}
