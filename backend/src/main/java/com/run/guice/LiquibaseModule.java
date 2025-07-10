package com.run.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/13  22:15}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class LiquibaseModule extends AbstractModule {
    @Inject
    @Named("run.server.datasource.database" )
    private String database;
    @Inject
    @Named("run.server.datasource.username" )
    private String username;
    @Inject
    @Named("run.server.datasource.password" )
    private String password;
    @Inject
    @Named("run.server.datasource.host" )
    private String host;

    @Inject
    @Named("run.server.datasource.port" )
    private Integer port;

    @Override
    protected void configure() {
        try {
            Class.forName("org.postgresql.Driver" );
            Connection connection = DriverManager.getConnection(String.format("jdbc:postgresql://%s:%d/%s", this.host, this.port, this.database), this.username, password);
            Database database = DatabaseFactory
                    .getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase("migrations/changelog.xml",
                    new ClassLoaderResourceAccessor(),
                    database);
            // 3. 执行数据库升级
            liquibase.update(new Contexts(), new LabelExpression());
            connection.close();
        } catch (SQLException | LiquibaseException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
