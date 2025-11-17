package com.run.dao.common.entity;

import com.run.dao.common.annotations.Table;
import com.run.dao.common.convert.EntityConvert;
import com.run.dao.common.convert.sqlite.SqliteConvert;
import org.jooq.SQLDialect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/13  23:39}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class EntityConfig<T> {
    private final Map<SQLDialect, Item<T>> config;

    private SQLDialect active;

    public EntityConfig(List<Item<T>> item, SQLDialect active) {
        config = item.stream().collect(Collectors.toMap(Item::sqlDialect, i -> i));
        this.active = active;
    }

    public EntityConfig(Class<T> clazz) {
        Table t = clazz.getAnnotation(Table.class);
        config = new HashMap<>();
        config.put(SQLDialect.SQLITE, new Item<>(SQLDialect.SQLITE, table(name(t.catalogName(), t.schemaName(), t.name())), new SqliteConvert<>(clazz)));
    }

    public org.jooq.Table<?> getTable(SQLDialect sqlDialect) {
        return config.get(sqlDialect).table;
    }

    public org.jooq.Table<?> getTable() {
        return config.get(active).table;
    }

    public EntityConvert<T> getConvert() {
        return config.get(active).convert;
    }


    public EntityConvert<T> getConvert(SQLDialect sqlDialect) {
        return config.get(sqlDialect).convert;
    }

    public static record Item<T>(SQLDialect sqlDialect, org.jooq.Table<?> table,
                                 EntityConvert<T> convert) {
    }
}
