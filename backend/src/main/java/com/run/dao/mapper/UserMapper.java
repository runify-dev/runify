package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.common.util.SqlGenUtil;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.User;
import io.vertx.sqlclient.Pool;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/13  23:57}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class UserMapper extends BaseMapper<User> {
    @Inject
    public UserMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
