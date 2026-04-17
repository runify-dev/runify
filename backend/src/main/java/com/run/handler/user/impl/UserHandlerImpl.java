package com.run.handler.user.impl;


import com.run.common.exception.ApiException;
import com.run.common.query.Query;
import com.run.common.result.Page;
import com.run.common.result.Result;
import com.run.common.util.CommonUtils;
import com.run.common.util.JWTUtil;
import com.run.common.util.ValidatorUtil;
import com.run.common.validator.Group;
import com.run.dao.common.F;
import com.run.dao.entity.User;
import com.run.dao.mapper.UserMapper;
import com.run.handler.user.IUserHandler;
import com.run.handler.user.dto.UserDTO;
import com.run.handler.user.pojo.LoginPojo;
import com.run.handler.user.vo.UserQueryVO;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
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
        user.setIcon("./user.jpeg");
        user.setPassword(CommonUtils.getSHA256(user.getPassword()));
        user.setRole("USER");
        ValidatorUtil.validate(user, Group.Create.class);
        userMapper.one(F.field(User::getUsername)
                                .eq(F.params(User::getUsername)),
                        Map.of("username", user.getUsername()))
                .compose(u -> {
                    if (u == null) {
                        return userMapper.save(user);
                    }
                    return Future.failedFuture("用户名存在");
                }).onSuccess(ok -> {
                    context.end(Result.success(user).toBuffer());
                }).onFailure(context::fail);
    }

    @Override
    public void deleteUser(RoutingContext context) {
        String userId = context.pathParam("id");
        if (Strings.CS.equals(userId, "22d90f6c-2092-43b8-aa14-d1f9731522ac")) {
            context.fail(new ApiException(500, "内置用户不允许删除"));
            return;
        }
        userMapper.deleteById(userId).onSuccess(ok -> {
            context.end(Result.success(true).toBuffer());
        }).onFailure(context::fail);
    }

    @Override
    public Handler<RoutingContext> login() {
        return context -> {
            LoginPojo loginPojo = context.body().asPojo(LoginPojo.class);
            // 校验参数
            ValidatorUtil.validate(loginPojo);
            String username = loginPojo.getUsername();
            String password = loginPojo.getPassword();


            Condition eq = F.field(User::getUsername)
                    .eq(F.params(User::getUsername))
                    .and(F.field(User::getPassword).eq(F.params(User::getPassword)));
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

    public UserDTO to(User user) {
        UserDTO userDto = new UserDTO();
        try {
            BeanUtils.copyProperties(userDto, user);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return userDto;
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

    public Condition getCondition(UserQueryVO query) {
        String mixing = query.getGlobal();
        Condition condition = DSL.noCondition();
        Field<String> username = F.field(User::getUsername);
        Field<String> nikiName = F.field(User::getNickname);
        if (StringUtils.isNotEmpty(mixing)) {
            Field<String> phone = F.field(User::getPhone);
            Field<String> email = F.field(User::getEmail);
            Param<String> mixingParams = DSL.param("global", String.class);
            condition = condition.and(username.like(mixingParams)
                    .or(nikiName.like(mixingParams)
                            .or(phone.like(mixingParams))
                            .or(email.like(mixingParams))));
        }
        if (StringUtils.isNotEmpty(query.getUsername())) {
            condition = condition.and(username.like(F.params(User::getUsername)));
        }
        if (StringUtils.isNotEmpty(query.getNickname())) {
            condition = condition.and(nikiName.like(F.params(User::getNickname)));
        }
        return condition;
    }

    @Override
    public void query(RoutingContext context) {
        UserQueryVO query = Query.format(UserQueryVO.class, context);
        if (query.getCurrentPage() != null && query.getPageSize() != null) {
            userMapper.page(getCondition(query), query.getCurrentPage(), query.getPageSize(),
                            CommonUtils.ofNullable("global", "%" + query.getGlobal() + "%",
                                    "username", "%" + query.getUsername() + "%",
                                    "nickname", "%" + query.getNickname() + "%"))
                    .onSuccess(userPage -> {
                        List<UserDTO> list = userPage.getRecords().stream().map(this::to).toList();
                        Page<UserDTO> result = new Page<>();
                        result.setRecords(list);
                        result.setSize(userPage.getSize());
                        result.setCurrent(userPage.getCurrent());
                        result.setTotal(userPage.getTotal());
                        context.end(Result.success(result).toBuffer());
                    }).onFailure(context::fail);
        } else {
            userMapper.list(getCondition(query),
                            CommonUtils.ofNullable("global", "%" + query.getGlobal() + "%",
                                    "username", "%" + query.getUsername() + "%",
                                    "nickname", "%" + query.getNickname() + "%"))
                    .onSuccess(users -> {
                        context.end(Result.success(users.stream().map(UserDTO::new).toList()).toBuffer());
                    }).onFailure(context::fail);
        }

    }


}
