package com.run.sql.example;

import com.run.sql.DSLContext;
import com.run.sql.RenderedSql;
import com.run.sql.dialect.SQLDialect;
import com.run.sql.model.ModelSetOptions;
import com.run.sql.model.Table;

import static com.run.sql.DSL.*;

public class Demo {
    public static void main(String[] args) {
        mysqlSelectDemo();
        pgSelectDemo();
        h2TableClassDemo();
        sqliteLambdaDemo();
        regexDemo();
        functionDemo();
        jsonDemo();
        modelSetDemo();
    }

    private static void mysqlSelectDemo() {
        DSLContext ctx = using(SQLDialect.MYSQL);

        RenderedSql sql = ctx.select(field("id"), field("name"))
                .from(table("user"))
                .where(field("id").in(
                        ctx.select(field("id"))
                                .from(table("user"))
                                .where(field("id").eq("你好"))
                ))
                .orderBy(field("create_time").desc())
                .limit(10)
                .offset(0)
                .render();

        print("mysqlSelectDemo", sql);
    }

    private static void pgSelectDemo() {
        DSLContext ctx = using(SQLDialect.POSTGRESQL);

        RenderedSql sql = ctx.select(field("id"), field("name"))
                .from(table("mydb", "public", "user"))
                .where(field("id").in(
                        ctx.select(field("id"))
                                .from(table("mydb", "public", "user_a"))
                                .where(field("id").eq("你好"))
                ))
                .orderBy(field("create_time").desc())
                .limit(10)
                .offset(0)
                .render();

        print("pgSelectDemo", sql);
    }

    private static void h2TableClassDemo() {
        DSLContext ctx = using(SQLDialect.H2);

        RenderedSql sql = ctx.select(field(UserModel::getId), field(UserModel::getName))
                .from(table(UserModel.class))
                .where(field(UserModel::getName).eq("张三"))
                .orderBy(field(UserModel::getCreateTime).desc())
                .limit(10)
                .offset(0)
                .render();

        print("h2TableClassDemo", sql);
    }

    private static void sqliteLambdaDemo() {
        DSLContext ctx = using(SQLDialect.SQLITE);
        Table users = table("main", "users");

        RenderedSql sql = ctx.select(field(UserModel::getId), field(UserModel::getName))
                .from(users)
                .where(users.field(UserModel::getName).eq("张三"))
                .orderBy(field(UserModel::getCreateTime).desc())
                .limit(10)
                .offset(0)
                .render();

        print("sqliteLambdaDemo", sql);
    }

    private static void regexDemo() {
        RenderedSql pg = using(SQLDialect.POSTGRESQL)
                .select(field("id"))
                .from(table("public", "user"))
                .where(field("name").regex("^zhang", false))
                .render();
        print("regexPostgresDemo", pg);

        RenderedSql mysql = using(SQLDialect.MYSQL)
                .select(field("id"))
                .from(table("user"))
                .where(field("name").regex("^zhang", false))
                .render();
        print("regexMysqlDemo", mysql);
    }

    private static void functionDemo() {
        RenderedSql sql = using(SQLDialect.MYSQL)
                .select(concat(field("first_name"), inline(" "), field("last_name")).as("full_name"),
                        currentTimestamp().as("now_time"))
                .from(table("user"))
                .render();
        print("functionDemo", sql);
    }

    private static void jsonDemo() {
        RenderedSql sql = using(SQLDialect.POSTGRESQL)
                .select(jsonText(field("data"), "$.name").as("name"))
                .from(table("public", "user"))
                .render();
        print("jsonDemo", sql);
    }

    private static void modelSetDemo() {
        DSLContext ctx = using(SQLDialect.POSTGRESQL);
        UserModel user = new UserModel("u001", "张三", 1710000000000L);

        RenderedSql insert = ctx.insertInto(user)
                .render();
        print("modelInsertDirectDemo", insert);

        RenderedSql insertWithClass = ctx.insertInto(UserModel.class)
                .set(user)
                .render();
        print("modelInsertClassDemo", insertWithClass);

        RenderedSql update = ctx.update(table(UserModel.class))
                .set(user, ModelSetOptions.excludePrimaryKeys())
                .where(field(UserModel::getId).eq(user.getId()))
                .render();
        print("modelUpdateSetDemo", update);

        RenderedSql directUpdate = ctx.update(user)
                .where(field(UserModel::getId).eq(user.getId()))
                .render();
        print("modelUpdateDirectDemo", directUpdate);
    }

    private static void print(String title, RenderedSql sql) {
        System.out.println("===== " + title + " =====");
        System.out.println(sql.sql());
        System.out.println(sql.params());
        System.out.println();
    }
}
