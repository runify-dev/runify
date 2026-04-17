package com.run.dao.common.mapper;


import com.run.common.config.AppConfig;
import com.run.common.result.Page;
import com.run.common.util.SqlTemplates;
import com.run.dao.common.F;
import com.run.dao.common.convert.EntityConvert;
import com.run.dao.common.entity.BaseEntity;
import com.run.dao.common.entity.EntityConfig;
import io.vertx.core.Future;
import io.vertx.sqlclient.*;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.templates.SqlTemplate;
import io.vertx.sqlclient.templates.TupleMapper;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.conf.ParamType;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;

import static org.jooq.impl.DSL.param;
import static org.jooq.impl.DSL.using;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/13  15:02}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class BaseMapper<T extends BaseEntity<T>> {
    Logger log = LoggerFactory.getLogger(this.getClass());


    private List<org.jooq.Field<?>> fields;

    @Getter
    private Table<?> table;
    @Getter
    protected Pool client;

    protected SQLDialect dbType;
    @Getter
    private DSLContext dslContext;

    private String saveTemplate;

    @Getter
    private org.jooq.Field<Object> primaryField;
    private EntityConfig<T> entityConfig;

    protected EntityConvert<T> getConvert() {
        return entityConfig.getConvert(dbType);
    }


    @SneakyThrows
    @Inject
    public BaseMapper(Pool client, AppConfig appConfig) {
        Class<T> entityClass = currentEntity();
        this.constructor(client, appConfig.getDatabase().getType(), entityClass);
    }

    @SneakyThrows
    public BaseMapper() {
        Class<T> entityClass = currentEntity();
        this.constructor(null, SQLDialect.SQLITE, entityClass);
    }

    @SneakyThrows
    public BaseMapper(Pool client, SQLDialect dbType, Class<T> entityClass) {
        this.constructor(client, dbType, entityClass);
    }

    @SneakyThrows
    private void constructor(Pool client, SQLDialect dbType, Class<T> entityClass) {
        this.client = client;
        this.dbType = dbType;
        this.dslContext = using(dbType, new Settings().withRenderNamedParamPrefix(""));
        this.entityConfig = F.getEntityConfig(entityClass);
        this.table = entityConfig.getTable(dbType);
        this.fields = F.getFieldList(entityClass);
        List<Field> fields = Arrays.stream(FieldUtils
                        .getFieldsWithAnnotation(entityClass, com.run.dao.common.annotations.Column.class))
                .filter(field -> field.getAnnotation(com.run.dao.common.annotations.Column.class).primaryKey()).toList();
        if (fields.size() != 1) {
            throw new RuntimeException("主键只能有一个");
        }
        this.primaryField = DSL.field(fields.getFirst().getName());

        this.saveTemplate = generateSaveTemplate();
    }

    private String generateSaveTemplate() {
        Map<org.jooq.Field<?>, Param<?>> updateMap = new HashMap<>();
        for (org.jooq.Field<?> field : fields) {
            updateMap.put(field, param("#{" + field.getName() + "}"));
        }
        return dslContext.insertInto(table)
                .set(updateMap).getSQL(ParamType.NAMED);
    }

    private String generateUpdateTemplate(T obj) {
        Map<String, Object> map = getConvert().toMap(obj);
        Map<org.jooq.Field<?>, Param<?>> updateMap = new HashMap<>();
        for (org.jooq.Field<?> field : fields) {
            if (!field.getName().equals(primaryField.getName())) {
                if (map.get(field.getName()) != null) {
                    updateMap.put(field, param("#{" + field.getName() + "}"));
                }
            }
        }
        return dslContext.update(table)
                .set(updateMap)
                .where(primaryField.eq(param("#{" + primaryField.getName() + "}")))
                .getSQL(ParamType.NAMED);

    }

    public Class<T> currentEntity() {
        ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();
        return (Class<T>) genericSuperclass.getActualTypeArguments()[0];
    }

    /**
     * 生成插入模板
     *
     * @return 查询模板
     */
    public SqlTemplate<T, SqlResult<Void>> generateInsertSqlTemplate() {
        return generateInsertSqlTemplate(client);
    }

    /**
     * 生成插入模板
     *
     * @return 查询模板
     */
    public SqlTemplate<T, SqlResult<Void>> generateInsertSqlTemplate(SqlClient client) {
        return SqlTemplate
                .forUpdate(client, saveTemplate)
                .mapFrom(TupleMapper.mapper(this.getConvert()::toMap));
    }

    /**
     * 插入
     *
     * @param t 实例对象
     * @return 异步响应
     */
    public Future<SqlResult<Void>> save(T t) {
        log.info("sql:{}\n{}", saveTemplate, this.getConvert().toMap(t));
        return generateInsertSqlTemplate().execute(t);
    }

    /**
     * 批量插入
     *
     * @param list 实例对象
     * @return 异步响应
     */
    public Future<SqlResult<Void>> batch_save(List<T> list) {
        return batch_save(list, client);
    }

    public Future<SqlResult<Void>> batch_save(List<T> list, SqlClient client) {
        return generateInsertSqlTemplate(client).executeBatch(list);
    }

    /**
     * 检索
     *
     * @param condition 查询
     * @param params    参数
     * @return 数据
     */
    public Future<RowSet<T>> search(Condition condition, Map<String, Object> params) {
        String template = dslContext.select(fields).from(table).where(condition).getSQL(ParamType.NAMED);
        return search(template, params);
    }

    public Future<RowSet<T>> search(Condition condition, Map<String, Object> params, SqlClient client) {
        String template = dslContext.select(fields).from(table).where(condition).getSQL(ParamType.NAMED);
        return search(template, params, client);
    }

    /**
     * 检索
     *
     * @param template 模版
     * @param params   参数
     * @return 数据
     */
    public Future<RowSet<T>> search(String template, Map<String, Object> params) {
        return SqlTemplate.forQuery(client, template)
                .mapTo(this.getConvert()::mapTo)
                .execute(params);
    }

    /**
     * 检索
     *
     * @param template 模版
     * @param params   参数
     * @param client   连接
     * @return 数据
     */
    public Future<RowSet<T>> search(String template, Map<String, Object> params, SqlClient client) {
        return SqlTemplate.forQuery(client, template)
                .mapTo(this.getConvert()::mapTo)
                .execute(params);
    }


    /**
     * 查询一条数据
     *
     * @param template 模版字符串
     * @param params   参数
     * @return 数据
     */
    public Future<T> one(String template, Map<String, Object> params) {
        return one(template, params, client);
    }

    /**
     * 查询一条数据
     *
     * @param condition 条件
     * @param params    条件数据
     * @return 数据
     */
    public Future<T> one(Condition condition, Map<String, Object> params) {
        return one(condition, params, client);
    }

    /**
     * 查询一条数据
     *
     * @param condition 条件
     * @param params    条件数据
     * @return 数据
     */
    public Future<T> one(Condition condition, Map<String, Object> params, SqlClient client) {
        String sql = dslContext
                .select(fields)
                .from(table)
                .where(condition)
                .getSQL(ParamType.NAMED);
        return one(sql, params, client);
    }

    /**
     * 查询一条数据
     *
     * @param template 模版字符串
     * @param params   参数
     * @return 数据
     */
    public Future<T> one(String template, Map<String, Object> params, SqlClient client) {
        return search(template, params, client).compose(row -> {
            int size = row.size();
            if (size == 1) {
                return Future.succeededFuture(row.iterator().next());
            } else if (size > 1) {
                return Future.failedFuture(new RuntimeException("数据大于1条"));
            }
            return Future.succeededFuture(null);
        });
    }

    /**
     * 根据主键id查询数据
     *
     * @param id 主键id
     * @return 数据
     */
    public Future<T> getById(String id) {
        return getById(id, client);
    }

    /**
     * 根据主键id查询数据
     *
     * @param id 主键id
     * @return 数据
     */
    public Future<T> getById(String id, SqlClient client) {
        String sql = dslContext
                .select(fields)
                .from(table)
                .where(primaryField.eq(param("#{" + primaryField.getName() + "}")))
                .getSQL(ParamType.NAMED);
        return one(sql, Map.of(primaryField.getName(), id), client);
    }


    public Future<SqlResult<Void>> deleteById(String id, SqlClient client) {
        String sql = this.dslContext
                .delete(table)
                .where(primaryField.eq(param("#{" + primaryField.getName() + "}")))
                .getSQL(ParamType.NAMED);
        return delete(sql, Map.of(primaryField.getName(), id), client);
    }

    /**
     * 根据id删除数据
     *
     * @param id id
     * @return SqlResult
     */
    public Future<SqlResult<Void>> deleteById(String id) {
        return deleteById(id, client);
    }

    /**
     * 删除数据
     *
     * @param condition 条件
     * @param params    参数
     * @return SqlResult
     */
    public Future<SqlResult<Void>> delete(Condition condition, Map<String, Object> params, SqlClient sqlClient) {
        String sql = dslContext.delete(table).where(condition).getSQL(ParamType.NAMED);
        return delete(sql, params, sqlClient);
    }

    /**
     * 删除数据
     *
     * @param template 删除字符串
     * @param params   参数
     * @return SqlResult
     */
    public Future<SqlResult<Void>> delete(String template, Map<String, Object> params, SqlClient sqlClient) {
        return SqlTemplate.forUpdate(sqlClient, template)
                .execute(params);
    }


    /**
     * 删除数据
     *
     * @param template 删除对象
     * @param params   参数
     * @return 异步响应
     */
    public Future<SqlResult<Void>> delete(String template, Map<String, Object> params) {
        return delete(template, params, client);
    }

    public Future<SqlResult<Void>> delete(Condition condition, Map<String, Object> params) {
        String template = dslContext.delete(table).where(condition).getSQL(ParamType.NAMED);
        return delete(template, params, client);
    }

    public Future<SqlResult<Void>> delete(Condition condition, Map<String, Object> params, List<String> listKey) {
        params = new HashMap<>(params);
        String template = dslContext.delete(table).where(condition).getSQL(ParamType.NAMED);
        for (String key : listKey) {
            List<?> lists = (List<?>) params.get(key);
            SqlTemplates.ExpandedSql expanded = SqlTemplates.expandIn(template, key, lists);
            template = expanded.sql();
            params.putAll(expanded.params());
        }
        return delete(template, params, client);
    }

    public Future<SqlResult<Void>> update(String template, Map<String, Object> params) {
        return SqlTemplate.forUpdate(client, template)
                .execute(params);
    }

    public Future<SqlResult<Void>> update(Map<org.jooq.Field<Object>, Param<Object>> fieldMap, Condition condition, Map<String, Object> params) {
        return update(fieldMap, condition, params, client);
    }

    public Future<SqlResult<Void>> update(Map<org.jooq.Field<Object>, Param<Object>> fieldMap, Condition condition, Map<String, Object> params, SqlClient client) {
        String template = dslContext.update(table).set(fieldMap).where(condition).getSQL(ParamType.NAMED);
        return SqlTemplate.forUpdate(client, template)
                .execute(params);
    }

    /**
     * 根据实例对象修改数据
     *
     * @param t 实例对象
     * @return SqlResult
     */
    public Future<SqlResult<Void>> update(T t) {
        String template = generateUpdateTemplate(t);
        return SqlTemplate.forUpdate(client, template)
                .mapFrom(TupleMapper.mapper(this.getConvert()::toMap))
                .execute(t);
    }


    /**
     * 查询列表
     *
     * @param condition 查询条件
     * @param params    参数
     * @return 异步响应
     */
    public Future<List<T>> list(Condition condition, Map<String, Object> params) {
        return _list(condition, params).compose(BaseMapper::toListFuture);
    }

    public Future<List<T>> list(String template, Map<String, Object> params) {
        return search(template, params).compose(BaseMapper::toListFuture);
    }

    /**
     * 查询列表
     *
     * @param condition 查询条件
     * @param params    参数
     * @return 异步响应
     */
    public Future<RowSet<T>> _list(Condition condition, Map<String, Object> params) {
        String sql = dslContext.select(fields).from(table).where(condition).getSQL(ParamType.NAMED);
        return SqlTemplate.forQuery(client, sql)
                .mapTo(this.getConvert()::mapTo)
                .execute(params);
    }

    public SelectJoinStep<Record> select() {
        return dslContext.select(fields).from(table);
    }

    /**
     * 分页查询
     *
     * @param condition   查询条件
     * @param currentPage 当前页
     * @param pageSize    每页大小
     * @return 分页数据
     */
    public Future<Page<T>> page(Condition condition, long currentPage, long pageSize, Map<String, Object> params) {
        Future<Long> count = count(condition, params);
        return _page(condition, new ArrayList<>(), currentPage, pageSize, params).compose(rowSet -> count.compose(c -> {
            List<T> ts = toList(rowSet);
            return Future.succeededFuture(new Page<T>(ts, c, currentPage, pageSize));
        }));
    }

    /**
     * 分页查询
     *
     * @param condition   查询条件
     * @param currentPage 当前页
     * @param pageSize    每页大小
     * @return 分页数据
     */
    public Future<Page<T>> page(Condition condition, Collection<? extends OrderField<?>> orderFields, long currentPage, long pageSize, Map<String, Object> params) {
        Future<Long> count = count(condition, params);
        return _page(condition, orderFields, currentPage, pageSize, params).compose(rowSet -> count.compose(c -> {
            List<T> ts = toList(rowSet);
            return Future.succeededFuture(new Page<T>(ts, c, currentPage, pageSize));
        }));
    }

    /**
     * 将RowSet<T> 转换为List<T>
     *
     * @param rowSet 需要转换的rowSet
     * @param <T>    列表中的数据类型
     * @return 转换后的List<T>
     */
    private static <T> List<T> toList(RowSet<T> rowSet) {
        List<T> result = new ArrayList<>();
        rowSet.forEach(result::add);
        return result;
    }

    private static <T> Future<List<T>> toListFuture(RowSet<T> rowSet) {
        return Future.succeededFuture(toList(rowSet));
    }

    /**
     * 分页查询
     *
     * @param condition   查询条件
     * @param currentPage 当前页
     * @param pageSize    每页大小
     * @return 分页结果
     */
    private Future<RowSet<T>> _page(Condition condition, Collection<? extends OrderField<?>> orderFields,
                                    long currentPage, long pageSize, Map<String, Object> params) {
        String sql = dslContext.select(fields).from(table).where(condition)
                .orderBy(orderFields)
                .offset(DSL.param("#{offset}", Integer.class))
                .limit(DSL.param("#{limit}", Integer.class))
                .getSQL(ParamType.NAMED);
        HashMap<String, Object> result = new HashMap<>(params);
        result.put("offset", (currentPage - 1) * pageSize);
        result.put("limit", pageSize);
        return SqlTemplate.forQuery(client, sql)
                .mapTo(this.getConvert()::mapTo)
                .execute(result);

    }

    /**
     * 获取count
     *
     * @param condition 条件
     * @return count 数
     */
    public Future<Long> count(Condition condition) {
        String sql = dslContext.selectCount().from(table)
                .where(condition).getQuery().toString();
        return SqlTemplate.forQuery(client, sql)
                .execute(Map.of()).compose(rows -> {
                    Row next = rows.iterator().next();
                    return Future.succeededFuture(next.getLong("count(*)"));
                });
    }

    /**
     * 获取count
     *
     * @param condition 条件
     * @return count 数
     */
    public Future<Long> count(Condition condition, Map<String, Object> params) {
        String sql = dslContext.selectCount().from(table)
                .where(condition).getSQL(ParamType.NAMED);
        return SqlTemplate.forQuery(client, sql)
                .execute(params).compose(rows -> {
                    Row next = rows.iterator().next();
                    Long aLong = next.getLong(0);
                    return Future.succeededFuture(aLong);
                });

    }

}
