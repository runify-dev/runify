package com.run.common.util;

import com.run.dao.common.entity.BaseEntity;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Values;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/15  23:44}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class SqlGenUtil {
    private static final Pattern pattern = Pattern.compile("[A-Z0-9]");

    public static Map<String, Object> toMap(Object o) {
        Class<?> aClass = o.getClass();
        Field[] fields = aClass.getDeclaredFields();
        Map<String, Object> result = new HashMap<>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(com.run.dao.common.annotations.Column.class)) {
                try {
                    field.setAccessible(true);
                    result.put(field.getName(), field.get(o).toString());
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return result;
    }

    /**
     * 生成插入的Sql
     *
     * @param table_name 表名
     * @param clazz      实体类
     * @return 插入sql语句
     */
    public static String generateInsertSql(String table_name, Class<?> clazz) {
        return generateInsertSql(new Table(table_name), clazz);
    }

    /**
     * 生成插入sql
     *
     * @param table 表名
     * @param clazz 实体类
     * @return 插入sql语句
     */
    public static String generateInsertSql(Table table, Class<?> clazz) {
        Field[] fields = FieldUtils.getAllFields(clazz);
        List<Column> columns = new ArrayList<>();
        ParenthesedExpressionList<Expression> expressions = new ParenthesedExpressionList<>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(com.run.dao.common.annotations.Column.class)) {
                com.run.dao.common.annotations.Column annotation = field.getAnnotation(com.run.dao.common.annotations.Column.class);
                columns.add(new Column("\"" + annotation.name() + "\""));
                expressions.add(new Column("#{" + annotation.name() + "}"));
            }
        }
        Insert insert = new Insert();
        insert.setTable(table);
        insert.withSelect(new Values()
                .withExpressions(expressions));
        insert.setColumns(new ExpressionList<>(columns));
        return insert.toString();
    }


    public static List<UpdateSet> getUpdateSetList(Map<String, Object> params) {
        return params.keySet().stream().map(key ->
                new UpdateSet(new Column("\"" + key + "\""),
                        new Column("#{" + key + "}"))
        ).toList();
    }

    public static <T extends BaseEntity<T>> List<UpdateSet> getUpdateSetList(T o) {
        Class<? extends BaseEntity> clazz = o.getClass();
        Field[] fields = FieldUtils.getAllFields(clazz);
        Map<String, Object> objectMap = o.toMap();
        List<UpdateSet> updateSetList = new ArrayList<>();
        for (Field field : fields) {
            com.run.dao.common.annotations.Column column = field.getAnnotation(com.run.dao.common.annotations.Column.class);
            if (field.isAnnotationPresent(com.run.dao.common.annotations.Column.class)) {
                Object value = objectMap.get(field.getName());
                if ((value instanceof String && StringUtils.isNotEmpty((String) value)) || value != null) {
                    if (!column.primaryKey()) {
                        UpdateSet updateSet = new UpdateSet(new Column("\"" + column.name() + "\""),
                                new Column("#{" + field.getName() + "}"));
                        updateSetList.add(updateSet);
                    }
                }
            }
        }
        return updateSetList;
    }

    public static Update generateUpdateSql(Table table, Map<String, Object> params, String primaryKey) {
        Update update = new Update();
        List<UpdateSet> updateSetList = new ArrayList<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getKey().equals(primaryKey)) {
                update.withWhere(new EqualsTo()
                        .withLeftExpression(new Column(entry.getKey()))
                        .withRightExpression(new Column("#{" + entry.getKey() + "}")));

            } else {
                UpdateSet updateSet = new UpdateSet(new Column("\"" + entry.getKey() + "\""),
                        new Column("#{" + entry.getKey() + "}"));
                updateSetList.add(updateSet);
            }
        }
        update.setUpdateSets(updateSetList);
        return update;
    }

    public static <T extends BaseEntity<T>> Update generateUpdateSql(Table table, T o) {
        Update update = new Update();
        Class<? extends BaseEntity> clazz = o.getClass();
        Field[] fields = FieldUtils.getAllFields(clazz);
        List<UpdateSet> updateSetList = new ArrayList<>();
        for (Field field : fields) {
            com.run.dao.common.annotations.Column column = field.getAnnotation(com.run.dao.common.annotations.Column.class);
            if (field.isAnnotationPresent(com.run.dao.common.annotations.Column.class)) {
                UpdateSet updateSet = new UpdateSet(new Column("\"" + column.name() + "\""),
                        new Column("#{" + column.name() + "}"));
                field.setAccessible(true);
                try {
                    Object value = field.get(o);
                    if ((value instanceof String && StringUtils.isNotEmpty((String) value)) || value != null) {
                        if (column.primaryKey()) {
                            update.withWhere(new EqualsTo()
                                    .withLeftExpression(new Column(field.getAnnotation(com.run.dao.common.annotations.Column.class).name()))
                                    .withRightExpression(new Column("#{" + field.getName() + "}"))
                            );
                        } else {
                            updateSetList.add(updateSet);
                        }
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        update.setTable(table);
        update.setUpdateSets(updateSetList);
        return update;
    }

    /**
     * 大驼峰转_ 命令
     *
     * @param str 需要转换的字符串
     * @return 转换后的字符串
     */
    private static String to_(String str) {
        Matcher matcher = pattern.matcher(str);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            if (sb.length() == 0) {
                matcher.appendReplacement(sb, matcher.group(0).toLowerCase());
            } else {
                matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
            }

        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}

