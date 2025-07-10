package com.run.auth.provider;

import com.auth0.jwt.interfaces.Claim;
import com.run.common.util.JWTUtil;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.impl.UserImpl;
import io.vertx.sqlclient.Pool;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.PlainSelect;

import java.util.List;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/4  23:50}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class TokenProvider implements AuthenticationProvider {
    private Pool pool;

    public TokenProvider(Pool pool) {
        this.pool = pool;
    }

    @Override
    public Future<User> authenticate(Credentials credentials) {
        TokenCredentials tokenCredentials = (TokenCredentials) credentials;
        String token = tokenCredentials.getToken();
        Map<String, Claim> data = JWTUtil.decodeToken(token);
        String userId = data.get("id").asString();
        PlainSelect plainSelect = new PlainSelect(List.of(
                new Column("*")),
                new Table("public", "user")
        );
        String selectUser = plainSelect
                .withWhere(new EqualsTo().withLeftExpression(new Column("id"))
                        .withRightExpression(new StringValue(userId)))
                .toString();
        return this.pool.query(selectUser)
                .mapping(com.run.dao.entity.User::new)
                .execute()
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
