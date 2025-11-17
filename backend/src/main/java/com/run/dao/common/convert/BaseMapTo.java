package com.run.dao.common.convert;

import com.run.dao.common.entity.BaseEntity;
import io.vertx.sqlclient.Row;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/14  00:02}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface BaseMapTo {
    <T extends BaseEntity<T>> T mapTo(Row row, Class<T> instance) throws NoSuchMethodException;
}
