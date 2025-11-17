package com.run.dao.common.constants;

import com.run.dao.common.annotations.Table;
import com.run.dao.common.convert.EntityConvert;
import com.run.dao.common.convert.Converter;
import com.run.dao.common.convert.postgres.PostgresConvert;
import com.run.dao.common.convert.sqlite.SqliteConvert;
import lombok.Getter;
import org.jooq.SQLDialect;
import org.jooq.Schema;
import org.jooq.impl.DSL;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.jooq.impl.DSL.table;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/15  21:47}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
public enum ConvertConstants {
    SQLITE(SQLDialect.SQLITE, SqliteConvert::new, t -> table(t.getName())),

    POSTGRES(SQLDialect.POSTGRES, PostgresConvert::new, t -> table(DSL.name(Optional.ofNullable(t.getSchema()).map(Schema::getName).orElse(null), t.getName())));

    ConvertConstants(SQLDialect sqlDialect, BiFunction<Class<?>,
                             Map<String, Converter<?, ?>>, EntityConvert<?>> newInstance,
                     Function<org.jooq.Table<?>, org.jooq.Table<?>> mappingTable) {
        this.sqlDialect = sqlDialect;
        this.newInstance = newInstance;
        this.mappingTable = mappingTable;

    }

    final SQLDialect sqlDialect;
    final Function<org.jooq.Table<?>, org.jooq.Table<?>> mappingTable;
    final BiFunction<Class<?>, Map<String, Converter<?, ?>>, EntityConvert<?>> newInstance;

}
