package com.run.dao.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/11  22:33}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.TYPE})
public @interface Table {
    /**
     * 表名
     *
     * @return 表名
     */
    String name();

    /**
     * @return catalog名称
     */
    String catalogName() default "";

    /**
     * @return schema名称
     */
    String schemaName() default "";
}
