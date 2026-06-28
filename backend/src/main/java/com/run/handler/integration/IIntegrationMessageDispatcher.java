package com.run.handler.integration;

import com.run.dao.entity.Integration;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/19  21:46}
 * {@code @Version 1.0}
 * {@code @注释: 入站消息分发器: 把 IM 平台收到的消息路由到集成绑定的应用并产生回复 }
 */
public interface IIntegrationMessageDispatcher {

    /**
     * 处理一条入站消息, 返回回复文本
     *
     * @param integration 命中的集成(含绑定的 applicationId)
     * @param fromUser    平台侧发送者标识
     * @param content     问题内容(与对话接口 ConversationVO.content 同构: {content, images, files, ...},
     *                    其中 images/files 为 [{url, name}])
     * @return 回复文本
     */
    Future<String> dispatch(Integration integration, String fromUser, JsonObject content);

    /**
     * 流式处理: 每产生一段文本回调 onDelta(增量), 最终 Future 完成时返回完整回复。
     * 默认不流式(等同 dispatch, 完成后一次性返回)。
     */
    default Future<String> dispatchStream(Integration integration, String fromUser, JsonObject content, Consumer<String> onDelta) {
        return dispatch(integration, fromUser, content);
    }

    /**
     * 分段处理: 按内容块 id 聚合, 每完成一段(出现新 id 或结束)回调 onSegment(type, content)。
     * 适合个人微信这种"只能发整条、想边产出边发"的场景。默认不分段(等同 dispatch)。
     */
    default Future<String> dispatchSegments(Integration integration, String fromUser, JsonObject content,
                                            BiConsumer<String, String> onSegment) {
        return dispatch(integration, fromUser, content);
    }
}
