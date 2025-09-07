package com.run.common.util;

import com.run.dao.common.annotations.Column;
import com.run.dao.entity.User;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Values;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.conf.ParamType;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import static org.jooq.impl.DSL.*;

public class FieldUtil {

    // 缓存字段解析结果，提高性能
    private static final ConcurrentMap<String, Field<?>> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Param<?>> PARMS_CACHE = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T, R> Field<R> getField(SFunction<T, R> function) {
        try {
            SerializedLambda lambda = getSerializedLambda(function);
            String cacheKey = generateCacheKey(lambda);
            // 先从缓存中获取
            Field<?> cachedField = FIELD_CACHE.get(cacheKey);
            if (cachedField != null) {
                return (Field<R>) cachedField;
            }
            // 解析字段信息
            FieldInfo fieldInfo = resolveFieldInfo(lambda);
            Field<R> field = (Field<R>) DSL.field(fieldInfo.columnName());

            // 放入缓存
            FIELD_CACHE.put(cacheKey, field);
            return field;

        } catch (Exception e) {
            throw new RuntimeException("获取字段失败: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T, R> Field<R> getParms(SFunction<T, R> function) {
        try {
            SerializedLambda lambda = getSerializedLambda(function);
            String cacheKey = generateCacheKey(lambda);
            // 先从缓存中获取
            Param<?> cachedParam = PARMS_CACHE.get(cacheKey);
            if (cachedParam != null) {
                return (Field<R>) cachedParam;
            }
            // 解析字段信息
            FieldInfo fieldInfo = resolveFieldInfo(lambda);
            Param<R> param = (Param<R>) DSL.param("#{" + fieldInfo.fieldName() + "}");

            // 放入缓存
            PARMS_CACHE.put(cacheKey, param);
            return param;

        } catch (Exception e) {
            throw new RuntimeException("获取字段失败: " + e.getMessage(), e);
        }
    }

    private static Class<?> getReturnType(SerializedLambda lambda) throws ClassNotFoundException {
        String implMethodName = lambda.getImplMethodName();

        // 获取实现类
        String implClass = lambda.getImplClass().replace('/', '.');
        Class<?> implClassObj = Class.forName(implClass);

        // 获取方法
        Method method = findMethod(implClassObj, implMethodName);
        return method.getReturnType();
    }

    private static Method findMethod(Class<?> clazz, String methodName) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }

    /**
     * 自定义函数接口（必须继承Serializable）
     */
    @FunctionalInterface
    public interface SFunction<T, R> extends Function<T, R>, Serializable {
    }

    private record FieldInfo(String fieldName, String columnName) {

    }

    /**
     * 获取SerializedLambda
     */
    private static SerializedLambda getSerializedLambda(Serializable function) {
        try {
            Method method = function.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(true);
            return (SerializedLambda) method.invoke(function);
        } catch (Exception e) {
            throw new RuntimeException("无法获取SerializedLambda", e);
        }
    }

    /**
     * 生成缓存key
     *
     * @param lambda 表达式
     * @return 缓存key
     */
    private static String generateCacheKey(SerializedLambda lambda) {
        return generateCacheKey(lambda.getImplClass(), lambda.getImplMethodName());
    }

    private static String generateCacheKey(String implClass, String implMethodName) {
        return implClass + "#" + implMethodName;
    }

    private static void register(String implClass, String implMethodName, Field<?> field) {
        FIELD_CACHE.put(generateCacheKey(implClass, implMethodName), field);
    }

    private static void registerParams(String implClass, String fieldName, Param<?> param) {
        PARMS_CACHE.put(generateCacheKey(implClass, fieldName), param);
    }

    /**
     * 解析字段信息，优先使用@Column注解
     *
     * @param lambda 表达式
     * @return 字段信息
     * @throws Exception 方法不存在异常
     */
    private static FieldInfo resolveFieldInfo(SerializedLambda lambda) throws Exception {
        String methodName = lambda.getImplMethodName();
        String className = lambda.getImplClass().replace("/", ".");

        Class<?> clazz = Class.forName(className);
        String fieldName = resolveFieldNameFromMethod(methodName);

        // 优先查找字段上的@Column注解
        try {
            java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
            Column column = field.getAnnotation(Column.class);
            if (column != null && !column.name().trim().isEmpty()) {
                return new FieldInfo(fieldName, column.name());
            }
        } catch (NoSuchFieldException e) {
            // 如果字段不存在，继续尝试通过getter方法查找
        }

        // 其次查找getter方法上的@Column注解
        Method method = clazz.getMethod(methodName);
        Column column = method.getAnnotation(Column.class);
        if (column != null && !column.name().trim().isEmpty()) {
            return new FieldInfo(fieldName, column.name());
        }

        // 最后使用默认的字段名转换（驼峰转下划线）
        return new FieldInfo(fieldName, camelToUnderline(fieldName));
    }

    /**
     * 从方法名解析字段名
     *
     * @param methodName 方法名
     * @return 解析后的名称
     */
    private static String resolveFieldNameFromMethod(String methodName) {
        if (methodName.startsWith("get")) {
            return uncapitalize(methodName.substring(3));
        } else if (methodName.startsWith("is")) {
            return uncapitalize(methodName.substring(2));
        } else if (methodName.startsWith("set")) {
            return uncapitalize(methodName.substring(3));
        }
        return methodName;
    }

    /**
     * 首字母小写
     *
     * @param str 首字母
     * @return 处理后的数据
     */
    private static String uncapitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * 驼峰命名转下划线命名
     *
     * @param camelCase 驼峰
     * @return 下划线命名
     */
    private static String camelToUnderline(String camelCase) {
        return StringUtils.join(
                StringUtils.splitByCharacterTypeCamelCase(camelCase),
                "_"
        ).toLowerCase();
    }

    /**
     * 清空缓存（用于开发时重新加载）
     */
    public static void clearCache() {
        FIELD_CACHE.clear();
    }

    public static Table<?> getTable(Class<?> clazz) {
        if (clazz.isAnnotationPresent(com.run.dao.common.annotations.Table.class)) {
            com.run.dao.common.annotations.Table annotation = clazz.getAnnotation(com.run.dao.common.annotations.Table.class);
            return DSL.table(DSL.name(annotation.schemaName(), annotation.name()));
        }
        return DSL.table(DSL.name(camelToUnderline(clazz.getName())));

    }

    public static List<Field<?>> getFieldList(Class<?> clazz) {
        String name = clazz.getName();
        String implClass = name.replace(".", "/");
        java.lang.reflect.Field[] fields = FieldUtils.getAllFields(clazz);
        List<Field<?>> result = new ArrayList<>();
        for (java.lang.reflect.Field field : fields) {
            if (field.isAnnotationPresent(com.run.dao.common.annotations.Column.class)) {
                com.run.dao.common.annotations.Column annotation = field.getAnnotation(com.run.dao.common.annotations.Column.class);
                Field<?> f = DSL.field(DSL.name(annotation.name()));
                register(implClass, "get" + field.getName(), f);
                result.add(f);
            } else {
                Field<?> f = DSL.field(DSL.name(camelToUnderline(field.getName())));
                register(implClass, "get" + field.getName(), f);
                result.add(f);
            }
            registerParams(implClass, field.getName(), param("#{" + field.getName() + "}"));
        }
        return result;
    }


    public static void main(String[] args) {
        Class<User> userClass = User.class;
        List<Field<?>> fieldList = getFieldList(userClass);
        Table<?> table = getTable(userClass);
        SelectConditionStep<Record> where = using(SQLDialect.SQLITE, new Settings().withRenderNamedParamPrefix(""))
                .select()
                .from(table)
                .where(getField(User::getCreateTime).eq(getParms(User::getCreateTime)));

        Map<String, Param<?>> params = where.getParams();
        System.out.println(params);
        System.out.println(fieldList);
        String sql = sql();
        System.out.println(sql);
    }

    public static String sql() {
        DSLContext dslContext = using(SQLDialect.SQLITE, new Settings().withRenderNamedParamPrefix(""));
        List<Field<?>> fields = getFieldList(User.class);
        Table<?> table = getTable(User.class);
        Map<org.jooq.Field<?>, Param<?>> updateMap = new HashMap<>();
        for (org.jooq.Field<?> field : fields) {
            updateMap.put(field, param("#{" + field.getName() + "}"));

        }
        return dslContext.insertInto(table)
                .set(updateMap).getSQL(ParamType.NAMED);
    }
}
