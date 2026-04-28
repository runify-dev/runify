package com.run.dao.common.constants;

import com.run.dao.common.convert.Converter;
import com.run.dao.common.convert.EntityConvert;
import com.run.dao.common.convert.postgres.PostgresConvert;
import com.run.dao.common.convert.sqlite.SqliteConvert;
import com.run.sql.dialect.SQLDialect;
import com.run.sql.model.Table;
import lombok.Getter;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;


/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/15  21:47}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
public enum ConvertConstants {
    SQLITE(SQLDialect.SQLITE, SqliteConvert::new, t -> t),

    POSTGRES(SQLDialect.POSTGRESQL, PostgresConvert::new, t -> t);

    ConvertConstants(SQLDialect sqlDialect, BiFunction<Class<?>,
                             Map<String, Converter<?, ?>>, EntityConvert<?>> newInstance,
                     Function<Table, Table> mappingTable) {
        this.sqlDialect = sqlDialect;
        this.newInstance = newInstance;
        this.mappingTable = mappingTable;

    }

    final SQLDialect sqlDialect;
    final Function<Table, Table> mappingTable;
    final BiFunction<Class<?>, Map<String, Converter<?, ?>>, EntityConvert<?>> newInstance;

}
