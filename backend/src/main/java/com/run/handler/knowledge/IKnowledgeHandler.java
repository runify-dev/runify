package com.run.handler.knowledge;

import com.run.handler.common.ITreeHandler;
import io.vertx.ext.web.RoutingContext;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/14  22:40}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface IKnowledgeHandler extends ITreeHandler {


    /**
     * 修改
     *
     * @param context 上下文
     */
    void edit(RoutingContext context);


}
