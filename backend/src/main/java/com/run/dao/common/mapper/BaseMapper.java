package com.run.dao.common.mapper;


import com.run.common.config.AppConfig;
import com.run.common.result.Page;
import com.run.common.util.CommonUtils;
import com.run.common.util.SqlTemplates;
import com.run.dao.common.annotations.Column;
import com.run.dao.common.convert.EntityConvert;
import com.run.dao.common.entity.BaseEntity;
import com.run.dao.common.entity.EntityConfig;
import com.run.sql.DSLContext;
import com.run.sql.RenderedSql;
import com.run.sql.condition.Condition;
import com.run.sql.dialect.SQLDialect;
import com.run.sql.model.Field;
import com.run.sql.model.Param;
import com.run.sql.model.SortField;
import com.run.sql.model.Table;
import com.run.sql.query.InsertQuery;
import com.run.sql.query.SelectQuery;
import io.vertx.core.Future;
import io.vertx.sqlclient.*;
import io.vertx.sqlclient.templates.SqlTemplate;
import io.vertx.sqlclient.templates.TupleMapper;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.reflect.ParameterizedType;
import java.util.*;

import static com.run.sql.DSL.*;


/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/13  15:02}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class BaseMapper<T extends BaseEntity<T>> {
    Logger log = LoggerFactory.getLogger(this.getClass());


    private List<Field<?>> fields;

    @Getter
    private Table table;
    @Getter
    protected Pool client;

    protected SQLDialect dbType;
    @Getter
    private DSLContext dslContext;

    private String saveTemplate;

    @Getter
    private com.run.sql.model.Field<?> primaryField;

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
        this.dslContext = using(dbType);
        this.entityConfig = new EntityConfig<>(entityClass);
        this.table = entityConfig.getTable(dbType);
        List<Field<?>> fields = new ArrayList<>();
        for (java.lang.reflect.Field field : FieldUtils
                .getFieldsWithAnnotation(entityClass, Column.class)) {
            Column annotation = field.getAnnotation(Column.class);
            if (annotation.primaryKey()) {
                this.primaryField = field(annotation.name());
            }
            fields.add(field(annotation.name()));
        }
        this.fields = fields;
        this.saveTemplate = generateSaveTemplate();

    }

    private String generateSaveTemplate() {
        Map<Field<?>, Param<?>> updateMap = new HashMap<>();
        for (Field<?> field : fields) {
            updateMap.put(field, param(field.nameOrExpression()));
        }
        return dslContext.insertInto(table)
                .set(updateMap).render().sql();
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
        InsertQuery insertQuery = dslContext.insertInto(t);
        log.info("sql:{}\n{}", insertQuery, insertQuery.getParams());
        return SqlTemplate.forUpdate(client, insertQuery.getSQL()).execute(insertQuery.getParams());
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
        RenderedSql render = dslContext.select(fields).from(table).where(condition).render();
        return search(render.sql(), CommonUtils.merge(render.params(), params));
    }

    public Future<RowSet<T>> search(Condition condition, Map<String, Object> params, SqlClient client) {
        RenderedSql render = dslContext.select(fields).from(table).where(condition).render();
        return search(render.sql(), CommonUtils.merge(render.params(), params), client);
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
        RenderedSql render = dslContext
                .select(fields)
                .from(table)
                .where(condition).render();
        return one(render.sql(), CommonUtils.merge(render.params(), params), client);
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
        RenderedSql render = dslContext
                .select(fields)
                .from(table)
                .where(primaryField.eq(id)).render();
        return one(render.sql(), render.params(), client);
    }


    public Future<SqlResult<Void>> deleteById(String id, SqlClient client) {
        RenderedSql render = this.dslContext
                .deleteFrom(table)
                .where(primaryField.eq(id)).render();
        return delete(render.sql(), render.params(), client);
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
        RenderedSql render = dslContext.deleteFrom(table).where(condition).render();
        return delete(render.sql(), CommonUtils.merge(render.params(), params), sqlClient);
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
        return delete(condition, params, client);
    }


    @NotNull
    private static Result getResult(Map<String, Object> params, List<String> expandInKeyList, String template) {
        params = new HashMap<>(params);
        for (String key : expandInKeyList) {
            List<?> lists = (List<?>) params.get(key);
            SqlTemplates.ExpandedSql expanded = SqlTemplates.expandIn(template, key, lists);
            template = expanded.sql();
            params.putAll(expanded.params());
        }
        return new Result(params, template);
    }

    private record Result(Map<String, Object> params, String template) {
    }

    public Future<SqlResult<Void>> update(String template, Map<String, Object> params) {
        return SqlTemplate.forUpdate(client, template)
                .execute(params);
    }

    public Future<SqlResult<Void>> update(Map<Field<Object>, Param<Object>> fieldMap, Condition condition, Map<String, Object> params) {
        return update(fieldMap, condition, params, client);
    }

    public Future<SqlResult<Void>> update(Map<Field<Object>, Param<Object>> fieldMap, Condition condition) {
        return update(fieldMap, condition, Map.of(), client);
    }

    public Future<SqlResult<Void>> update(Map<Field<Object>, Param<Object>> fieldMap, Condition condition, Map<String, Object> params, SqlClient client) {
        RenderedSql render = dslContext.update(table).set(fieldMap).where(condition).render();
        return SqlTemplate.forUpdate(client, render.sql())
                .execute(CommonUtils.merge(render.params(), params));
    }

    /**
     * 根据实例对象修改数据
     *
     * @param t 实例对象
     * @return SqlResult
     */
    public Future<SqlResult<Void>> update(T t) {
        RenderedSql render = dslContext.update(table).set(t).render();
        String sql = render.sql();
        return SqlTemplate.forUpdate(client, sql)
                .mapFrom(TupleMapper.mapper(this.getConvert()::toMap))
                .execute(t);
    }

    public Future<List<T>> list(Condition condition) {
        return _list(condition, Map.of()).compose(BaseMapper::toListFuture);
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

    public Future<List<T>> list(RenderedSql renderedSql) {
        return search(renderedSql.sql(), renderedSql.params()).compose(BaseMapper::toListFuture);
    }

    /**
     * 查询列表
     *
     * @param condition 查询条件
     * @param params    参数
     * @return 异步响应
     */
    public Future<RowSet<T>> _list(Condition condition, Map<String, Object> params) {
        RenderedSql render = dslContext.select(fields).from(table).where(condition).render();
        return SqlTemplate.forQuery(client, render.sql())
                .mapTo(this.getConvert()::mapTo)
                .execute(CommonUtils.merge(render.params(), params));
    }


    public SelectQuery select() {
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
    public Future<Page<T>> page(Condition condition, Collection<? extends SortField> orderFields, long currentPage, long pageSize, Map<String, Object> params) {
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
    private Future<RowSet<T>> _page(Condition condition, Collection<? extends SortField> orderFields,
                                    long currentPage, long pageSize, Map<String, Object> params) {
        long offset = (currentPage - 1) * pageSize;
        RenderedSql render = dslContext.select(fields).from(table).where(condition)
                .orderBy(orderFields)
                .offset(offset)
                .limit(pageSize).render();
        return SqlTemplate.forQuery(client, render.sql())
                .mapTo(this.getConvert()::mapTo)
                .execute(CommonUtils.merge(render.params(), params));

    }

    /**
     * 获取count
     *
     * @param condition 条件
     * @return count 数
     */
    public Future<Long> count(Condition condition) {
        RenderedSql render = dslContext.selectCount().from(table)
                .where(condition).render();
        return SqlTemplate.forQuery(client, render.sql())
                .execute(render.params()).compose(rows -> {
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
        RenderedSql render = dslContext.selectCount().from(table)
                .where(condition).render();
        return SqlTemplate.forQuery(client, render.sql())
                .execute(CommonUtils.merge(render.params(), params)).compose(rows -> {
                    Row next = rows.iterator().next();
                    Long aLong = next.getLong(0);
                    return Future.succeededFuture(aLong);
                });

    }

}
