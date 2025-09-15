package com.run.handler.user.impl;


import com.run.common.result.Page;
import com.run.common.result.Result;
import com.run.common.util.*;
import com.run.common.validator.Group;
import com.run.dao.entity.User;
import com.run.dao.mapper.UserMapper;
import com.run.handler.user.IUserHandler;
import com.run.handler.user.pojo.LoginPojo;
import com.run.handler.user.pojo.UserPojo;
import com.run.handler.user.pojo.UserQueryPojo;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Param;
import org.jooq.impl.DSL;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    public void createUser(RoutingContext context) {
        User user = context.body().asPojo(User.class);
        user.setId(UUID.randomUUID());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setIcon("/ui/user.jpeg");
        ValidatorUtil.validate(user, Group.Create.class);
        userMapper.one(FieldUtil.getField(User::getUsername)
                                .eq(FieldUtil.getParms(User::getUsername)),
                        Map.of("username", user.getUsername()))
                .compose(u -> {
                    if (u == null) {
                        return userMapper.save(user);
                    }
                    return Future.failedFuture("用户名存在");
                }).onSuccess(ok -> {
                    context.end(Result.success(user).toBuffer());
                }).onFailure(context::fail); ;

    }

    @Override
    public Handler<RoutingContext> login() {
        return context -> {
            LoginPojo loginPojo = context.body().asPojo(LoginPojo.class);
            // 校验参数
            ValidatorUtil.validate(loginPojo);
            String username = loginPojo.getUsername();
            String password = loginPojo.getPassword();


            Condition eq = FieldUtil.getField(User::getUsername)
                    .eq(FieldUtil.getParms(User::getUsername))
                    .and(FieldUtil.getField(User::getPassword).eq(FieldUtil.getParms(User::getPassword)));
            userMapper.search(eq, Map.of("username", username, "password", CommonUtils.getSHA256(password)))
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

    public UserPojo to(User user) {
        UserPojo userPojo = new UserPojo();
        try {
            BeanUtils.copyProperties(userPojo, user);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return userPojo;
    }

    @Override
    public Handler<RoutingContext> profile() {
        return context -> {
            io.vertx.ext.auth.User user = context.user();
            User userInstance = user.get("user");
            context.end(Result.success(to(userInstance)).toBuffer());
        };

    }

    @Override
    public Handler<RoutingContext> logout() {
        return context -> {
            context.end(Result.success("已成功退出登录").toBuffer());
        };
    }

    public Condition getCondition(UserQueryPojo queryPojo) {
        String mixing = queryPojo.getMixing();
        Condition condition = DSL.noCondition();
        Field<String> username = FieldUtil.getField(User::getUsername);
        Field<String> nikiName = FieldUtil.getField(User::getNickname);
        if (StringUtils.isNotEmpty(mixing)) {
            Field<String> phone = FieldUtil.getField(User::getPhone);
            Field<String> email = FieldUtil.getField(User::getEmail);
            Param<String> mixingParams = FieldUtil.getParms(UserQueryPojo::getMixing);
            condition = condition.and(username.like(mixingParams)
                    .or(nikiName.like(mixingParams)
                            .or(phone.like(mixingParams))
                            .or(email.like(mixingParams))));
        }
        if (StringUtils.isNotEmpty(queryPojo.getNickname())) {
            condition = condition.and(username.like(FieldUtil.getParms(User::getUsername)));
        }
        if (StringUtils.isNotEmpty(queryPojo.getNickname())) {
            condition = condition.and(username.like(FieldUtil.getParms(User::getNickname)));
        }
        return condition;
    }

    @Override
    public void page(RoutingContext context) {
        MultiMap entries = context.queryParams();
        String currentPage = context.pathParam("currentPage");
        String pageSize = context.pathParam("pageSize");
        UserQueryPojo userQueryPojo = new UserQueryPojo(entries);
        userMapper.page(getCondition(userQueryPojo), Long.parseLong(currentPage), Long.parseLong(pageSize), userQueryPojo.toMap())
                .onSuccess(userPage -> {
                    List<UserPojo> list = userPage.getRecords().stream().map(this::to).toList();
                    Page<UserPojo> result = new Page<>();
                    result.setRecords(list);
                    result.setSize(userPage.getSize());
                    result.setCurrent(userPage.getCurrent());
                    result.setTotal(userPage.getTotal());
                    context.end(Result.success(result).toBuffer());
                }).onFailure(context::fail);
    }


}
