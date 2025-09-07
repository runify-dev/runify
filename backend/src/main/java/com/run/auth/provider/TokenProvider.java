package com.run.auth.provider;

import com.auth0.jwt.interfaces.Claim;
import com.run.common.util.JWTUtil;
import com.run.dao.common.mapper.BaseMapper;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.impl.UserImpl;
import io.vertx.sqlclient.Pool;
import org.jooq.SQLDialect;

import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/4  23:50}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class TokenProvider implements AuthenticationProvider {
    private Pool pool;

    private SQLDialect dbType;
    private BaseMapper<com.run.dao.entity.User> userMapper;

    private BaseMapper<com.run.dao.entity.User> getUserMapper() {
        if (userMapper == null) {
            this.userMapper = new BaseMapper<>(pool, dbType, com.run.dao.entity.User.class);
        }
        return this.userMapper;
    }

    public TokenProvider(Pool pool, SQLDialect dbType) {
        this.pool = pool;
        this.dbType = dbType;
    }

    @Override
    public Future<User> authenticate(Credentials credentials) {
        TokenCredentials tokenCredentials = (TokenCredentials) credentials;
        String token = tokenCredentials.getToken();
        Map<String, Claim> data = JWTUtil.decodeToken(token);
        String userId = data.get("id").asString();
        return getUserMapper().getById(userId)
                .compose(user -> {
                    if (user == null) {
                        return Future.failedFuture(new RuntimeException("用户不存在"));
                    }
                    return Future.succeededFuture(new UserImpl(new JsonObject(Map.of("user", user)), new JsonObject()));
                });

    }
}
