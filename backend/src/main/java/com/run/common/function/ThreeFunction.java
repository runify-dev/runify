package com.run.common.function;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/10/30  18:34}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface ThreeFunction<A, B, C, R> {
    R apply(A a, B b, C c);
}
