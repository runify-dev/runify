package com.run.common.util;

import com.run.auth.constants.TokenTypeConstants;
import io.vertx.core.json.JsonObject;

/**
 * 会话用户：贯穿工作流执行的用户身份 + 可选展示信息。
 * <p>
 * {@code userId} / {@code userType} 恒有——scope 路由（便签落 user 档还是 conversation 档）依赖它们，
 * 匿名用户（{@link TokenTypeConstants#ANONYMOUS}）无跨对话稳定身份，落 user 档时需降级。
 * {@code profile} 是可选的白名单展示字段（name/email/…，非匿名才有），供个性化节点引用；
 * <b>只放展示字段，绝不含角色/权限</b>（会泄漏进 prompt、撑大 token）。
 * <p>
 * 仅用于 conversation 工作流；processor(HTTP) 无会话用户语义，不传。
 */
public record ConversationUser(String userId, TokenTypeConstants userType, JsonObject profile) {

    public boolean isRegistered() {
        return userType == TokenTypeConstants.USER;
    }

    /**
     * 进 params / start-node 输出的形态：{@code {id, type, profile}}，与本 record 结构 1:1。
     * 身份/路由键（id、type）在顶层，展示字段收在 profile 子对象（匿名时为空对象）。
     */
    public JsonObject toParam() {
        JsonObject user = new JsonObject().put("id", userId);
        if (userType != null) {
            user.put("type", userType.name());
        }
        user.put("profile", profile == null ? new JsonObject() : profile.copy());
        return user;
    }
}
