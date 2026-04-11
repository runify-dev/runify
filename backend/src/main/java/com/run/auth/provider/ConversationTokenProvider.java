package com.run.auth.provider;

import com.run.auth.dto.ConversationTokenDTO;
import com.run.common.constants.ConversationUserConstants;
import com.run.common.util.JWTUtil;
import com.run.dao.common.annotations.Column;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.impl.UserImpl;

import java.util.Map;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/10  16:57}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ConversationTokenProvider implements AuthenticationProvider {
    @Override
    public Future<User> authenticate(Credentials credentials) {
        TokenCredentials tokenCredentials = (TokenCredentials) credentials;
        String token = tokenCredentials.getToken();
        ConversationTokenDTO conversationTokenDTO = ConversationTokenDTO.newInstance(token);
        String id = conversationTokenDTO.getId();
        return Future.succeededFuture(new UserImpl(new JsonObject(Map.of(
                "type", conversationTokenDTO.getType(),
                "conversationUserId", id,
                "conversationUserType", ConversationUserConstants.ANONYMOUS_USER,
                "applicationId", conversationTokenDTO.getApplicationId())), new JsonObject()));

    }
}
