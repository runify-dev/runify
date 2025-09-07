package com.run.dao.common.entity;

import com.run.dao.common.convert.BaseConvert;
import org.jooq.SQLDialect;

import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/13  15:13}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface BaseEntity<T> {
    default BaseConvert<T> getConvert(SQLDialect dbType) {
        return getConvertMap().get(dbType);
    }

    Map<SQLDialect, BaseConvert<T>> getConvertMap();
}
