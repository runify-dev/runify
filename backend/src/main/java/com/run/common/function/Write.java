package com.run.common.function;

import java.io.IOException;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/8/14  23:40}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface Write<M, N, T, U> {
    /**
     * @param m 工作流管理器
     * @param n 节点
     * @param t chunk数据
     * @param u 是否已结束
     */
    void write(M m, N n, T t, U u);
}
