package com.run.common.encoder;

import io.vertx.core.json.JsonArray;
import io.vertx.jdbcclient.impl.actions.JDBCColumnDescriptor;
import io.vertx.jdbcclient.spi.JDBCColumnDescriptorProvider;
import io.vertx.jdbcclient.spi.JDBCEncoderImpl;
import io.vertx.sqlclient.Tuple;

import java.sql.JDBCType;
import java.sql.SQLException;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/8/28  23:27}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class SqliteJDBCEncoderImpl extends JDBCEncoderImpl {
    @Override
    public Object encode(JsonArray input, int pos, JDBCColumnDescriptorProvider provider) throws SQLException {
        return super.encode(input, pos, provider);
    }

    @Override
    public Object encode(Tuple input, int pos, JDBCColumnDescriptorProvider provider) throws SQLException {
        Object value = input.getValue(pos - 1);
        if (value instanceof String) {
            return super.doEncode(JDBCColumnDescriptor.wrap(JDBCType.VARCHAR), value);
        } else if (value instanceof Integer) {
            return super.doEncode(JDBCColumnDescriptor.wrap(JDBCType.INTEGER), value);
        } else if (value instanceof Double) {
            return super.doEncode(JDBCColumnDescriptor.wrap(JDBCType.DOUBLE), value);
        } else if (value instanceof Float) {
            return super.doEncode(JDBCColumnDescriptor.wrap(JDBCType.FLOAT), value);
        }
        return super.encode(input, pos, provider);
    }
}
