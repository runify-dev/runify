package com.run.handler.user.impl;


import com.run.common.result.Result;
import com.run.common.util.CommonUtils;
import com.run.common.util.JWTUtil;
import com.run.common.util.SqlGenUtil;
import com.run.common.util.ValidatorUtil;
import com.run.common.validator.Group;
import com.run.dao.entity.User;
import com.run.dao.mapper.UserMapper;
import com.run.handler.user.IUserHandler;
import com.run.handler.user.pojo.LoginPojo;
import com.run.handler.user.pojo.UserPojo;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.templates.SqlTemplate;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.apache.commons.beanutils.BeanUtils;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/5  16:53}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class UserHandlerImpl implements IUserHandler {


    protected Pool pool;

    protected UserMapper userMapper;

    @Inject
    public UserHandlerImpl(Pool pool, UserMapper userMapper) {
        this.pool = pool;
        this.userMapper = userMapper;
    }

    @Override
    public Handler<RoutingContext> createUser() {
        return context -> {
            User user = context.body().asPojo(User.class);
            String sql = SqlGenUtil.generateInsertSql("user", User.class);
            ValidatorUtil.validate(user, Group.Create.class);
            SqlTemplate.forUpdate(pool, sql).mapFrom(User.class)
                    .executeBatch(List.of(user))
                    .onSuccess(ok -> {
                        context.end(Result.success(user).toBuffer());
                    }).onFailure(throwable -> {
                        context.end(Result.success(throwable.toString()).toBuffer());
                    });

        };
    }

    @Override
    public Handler<RoutingContext> login() {
        return context -> {
            LoginPojo loginPojo = context.body().asPojo(LoginPojo.class);
            // 校验参数
            ValidatorUtil.validate(loginPojo);
            String username = loginPojo.getUsername();
            String password = loginPojo.getPassword();


            AndExpression where = new AndExpression()
                    .withLeftExpression(new EqualsTo().withLeftExpression(new Column("username"))
                            .withRightExpression(new StringValue(username)))
                    .withRightExpression(new EqualsTo().withLeftExpression(new Column("password"))
                            .withRightExpression(new StringValue(CommonUtils.getSHA256(password))));


            userMapper.search(where, Map.of())
                    .onSuccess(rows -> {
                        if (rows.size() > 0) {
                            User user = rows.iterator().next();
                            String token = JWTUtil.getToken(Map.of("id", user.getId().toString()));
                            context.end(Result.success(token).toBuffer());
                        } else {
                            context.end(Result.error("用户名或者密码错误").toBuffer());
                        }
                    }).onFailure(e -> {
                        context.end(Result.error(e.toString()).toBuffer());
                    });

        };
    }

    @Override
    public Handler<RoutingContext> profile() {
        return context -> {
            io.vertx.ext.auth.User user = context.user();
            User userInstance = user.get("user");
            UserPojo userPojo = new UserPojo();
            try {
                BeanUtils.copyProperties(userPojo, userInstance);
                context.end(Result.success(userPojo).toBuffer());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

    }
}
