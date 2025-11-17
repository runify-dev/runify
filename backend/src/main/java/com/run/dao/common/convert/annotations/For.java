package com.run.dao.common.convert.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/15  15:28}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface For {
    Class<?>[] value();
}