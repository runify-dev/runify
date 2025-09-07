package com.run.dao.tables;

import org.jooq.Field;
import org.jooq.Table;

import static org.jooq.impl.DSL.*;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/8/29  23:35}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class User {
    static class Pgsql {
        public static Field<?> getPrimaryKey() {
            return Pgsql.ID;
        }

        public static final Table<?> USER = table(name("public", "user"));
        public static final Field<Long> ID = field(name("public", "id"), Long.class);
        public static final Field<String> USERNAME = field("username", String.class);
        public static final Field<String> EMAIL = field("email", String.class);
        public static final Field<String> STATUS = field("status", String.class);
    }

    static class SQLite {
        public static final Table<?> USERS = table("users");
        public static final Field<Long> ID = field("id", Long.class);
        public static final Field<String> USERNAME = field("username", String.class);
        public static final Field<String> EMAIL = field("email", String.class);
        public static final Field<String> STATUS = field("status", String.class);
    }
}
