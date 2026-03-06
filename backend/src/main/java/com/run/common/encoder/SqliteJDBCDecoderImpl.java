package com.run.common.encoder;

import io.vertx.jdbcclient.impl.actions.JDBCColumnDescriptor;
import io.vertx.jdbcclient.impl.actions.SQLValueProvider;
import io.vertx.jdbcclient.spi.JDBCDecoderImpl;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Types;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/3/7  00:51}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class SqliteJDBCDecoderImpl extends JDBCDecoderImpl {

    @Override
    protected Object decodeNumber(SQLValueProvider valueProvider, JDBCColumnDescriptor descriptor) throws SQLException {
        Object raw;
        try {
            // 直接用 null class 获取原始值，跳过 SQLite 不支持的类型强转
            raw = valueProvider.apply(null);
        } catch (SQLException e) {
            // getObject(pos) 本身也失败了，说明该列值完全无法读取
            return null;
        }

        if (raw == null) {
            return null;
        }

        // 如果 SQLite 直接返回了正确的数字类型，走正常 cast
        if (raw instanceof Number) {
            return cast(raw);
        }

        // SQLite 弱类型：数字列存了字符串，手动转换
        String strVal = raw.toString().trim();
        if (strVal.isEmpty()) {
            return null;
        }

        try {
            int jdbcType = descriptor.jdbcType().getVendorTypeNumber();
            return switch (jdbcType) {
                case Types.TINYINT, Types.SMALLINT, Types.INTEGER -> Integer.parseInt(strVal);
                case Types.BIGINT -> Long.parseLong(strVal);
                case Types.FLOAT -> Float.parseFloat(strVal);
                case Types.REAL, Types.DOUBLE -> Double.parseDouble(strVal);
                case Types.NUMERIC, Types.DECIMAL -> new BigDecimal(strVal);
                default -> {
                    // 兜底：依次尝试 Integer → Long → Double
                    try {
                        yield Integer.parseInt(strVal);
                    } catch (NumberFormatException e1) {
                        try {
                            yield Long.parseLong(strVal);
                        } catch (NumberFormatException e2) {
                            try {
                                yield Double.parseDouble(strVal);
                            } catch (NumberFormatException e3) {
                                yield null;
                            }
                        }
                    }
                }
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }
}