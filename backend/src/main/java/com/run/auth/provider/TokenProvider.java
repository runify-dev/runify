package com.run.auth.provider;

import com.auth0.jwt.interfaces.Claim;
import com.run.common.constants.DatabaseType;
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
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;

import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/4  23:50}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class TokenProvider implements AuthenticationProvider {
    private Pool pool;

    private DatabaseType dbType;
    private BaseMapper<com.run.dao.entity.User> userMapper;

    private BaseMapper<com.run.dao.entity.User> getUserMapper() {
        if (userMapper == null) {
            this.userMapper = new BaseMapper<>(pool, dbType, com.run.dao.entity.User.class);
        }
        return this.userMapper;
    }

    public TokenProvider(Pool pool, DatabaseType dbType) {
        this.pool = pool;
        this.dbType = dbType;
    }

    @Override
    public Future<User> authenticate(Credentials credentials) {
        TokenCredentials tokenCredentials = (TokenCredentials) credentials;
        String token = tokenCredentials.getToken();
        Map<String, Claim> data = JWTUtil.decodeToken(token);
        String userId = data.get("id").asString();


        Expression where = new EqualsTo().withLeftExpression(new Column("id"))
                .withRightExpression(new StringValue(userId));

        return getUserMapper().search(where, Map.of())
                .compose(rows -> {
                    int size = rows.size();
                    if (size == 1) {
                        com.run.dao.entity.User user = rows.iterator().next();
                        return Future.succeededFuture(new UserImpl(new JsonObject(Map.of("user", user)), new JsonObject()));
                    } else if (size > 1) {
                        return Future.failedFuture(new RuntimeException("数据大于1条"));
                    }
                    return Future.failedFuture(new RuntimeException("用户不存在"));
                });

    }
}
