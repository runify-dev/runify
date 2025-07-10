package com.run.dao.common.mapper;

import com.google.inject.Inject;
import com.run.common.result.Page;
import com.run.common.util.SqlGenUtil;
import com.run.dao.common.entity.BaseEntity;
import io.vertx.core.Future;
import io.vertx.sqlclient.*;
import io.vertx.sqlclient.templates.SqlTemplate;
import io.vertx.sqlclient.templates.TupleMapper;
import lombok.SneakyThrows;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.SelectUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/13  15:02}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public abstract class BaseMapper<T extends BaseEntity<T>> {


    private final T entity;

    private final Table table;
    @Inject
    protected Pool client;

    private final String saveTemplate;
    private final Field primaryField;

    Logger log = LoggerFactory.getLogger(this.getClass());

    public Table getTable() {
        return table;
    }

    @SneakyThrows
    public BaseMapper() {
        Class<T> entityClass = currentEntity();
        this.entity = entityClass.getConstructor().newInstance();
        com.run.dao.common.annotations.Table t = entityClass.getAnnotation(com.run.dao.common.annotations.Table.class);
        this.table = new Table(t.catalogName(), t.schemaName(), t.name());
        this.saveTemplate = SqlGenUtil.generateInsertSql(table, entityClass);
        List<Field> fields = Arrays.stream(FieldUtils
                        .getFieldsWithAnnotation(entityClass, com.run.dao.common.annotations.Column.class))
                .filter(field -> field.getAnnotation(com.run.dao.common.annotations.Column.class).primaryKey()).toList();
        if (fields.size() != 1) {
            throw new RuntimeException("主键只能有一个");
        }
        this.primaryField = fields.get(0);
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
        return SqlTemplate
                .forUpdate(client, saveTemplate)
                .mapFrom(TupleMapper.mapper(T::toMap));
    }


    /**
     * 插入
     *
     * @param t 实例对象
     * @return 异步响应
     */
    public Future<SqlResult<Void>> save(T t) {
        log.info("sql:{}\n{}", saveTemplate, t.toMap());
        return generateInsertSqlTemplate().execute(t);
    }

    /**
     * 批量插入
     *
     * @param list 实例对象
     * @return 异步响应
     */
    public Future<SqlResult<Void>> batch_save(List<T> list) {
        return generateInsertSqlTemplate().executeBatch(list);
    }

    /**
     * 查询
     *
     * @param select 查询条件
     * @param params 参数
     * @return 异步响应
     */
    public Future<RowSet<T>> search(Select select, Map<String, Object> params) {
        String template = select.getPlainSelect().toString();
        log.info("sql:{}\n{}", template, params);
        return SqlTemplate.forQuery(client, select.getPlainSelect().toString())
                .mapTo(entity::mapTo)
                .execute(params);

    }

    /**
     * 查询
     *
     * @param select 查询
     * @return 异步响应
     */
    public Future<RowSet<T>> search(Select select) {
        return search(select, Map.of());
    }

    /**
     * 查询
     *
     * @param where 条件
     * @return 数据
     */
    public Future<RowSet<T>> search(Expression where, Map<String, Object> params) {
        Select select = SelectUtils.buildSelectFromTable(table);
        PlainSelect plainSelect = select.getPlainSelect();
        plainSelect.withWhere(where);
        return search(select, params);
    }

    /**
     * 查询一条数据
     *
     * @param where 条件
     * @return 数据
     */
    public Future<T> one(Expression where) {
        Select select = SelectUtils.buildSelectFromTable(table);
        PlainSelect plainSelect = select.getPlainSelect();
        plainSelect.withWhere(where);
        return one(select);
    }

    /**
     * 根据主键id查询数据
     *
     * @param id 主键id
     * @return 数据
     */
    public Future<T> getById(String id) {
        Select select = SelectUtils.buildSelectFromTable(table);
        PlainSelect plainSelect = select.getPlainSelect();
        String name = this.primaryField.getAnnotation(com.run.dao.common.annotations.Column.class).name();
        EqualsTo equalsTo = new EqualsTo().withLeftExpression(new Column(name))
                .withRightExpression(new Column("#{%s}".formatted(name)));
        plainSelect.withWhere(equalsTo);
        return one(equalsTo, Map.of(name, id));
    }

    /**
     * 查询一条数据
     *
     * @param where  条件
     * @param params 条件数据
     * @return 数据
     */
    public Future<T> one(Expression where, Map<String, Object> params) {
        Select select = SelectUtils.buildSelectFromTable(table);
        PlainSelect plainSelect = select.getPlainSelect();
        plainSelect.withWhere(where);
        return one(select, params);
    }

    public Future<SqlResult<Void>> deleteById(String id, SqlClient sqlClient) {
        String name = this.primaryField.getAnnotation(com.run.dao.common.annotations.Column.class).name();
        EqualsTo equalsTo = new EqualsTo().withLeftExpression(new Column(name))
                .withRightExpression(new Column("#{%s}".formatted(name)));
        return delete(equalsTo, Map.of(name, id));
    }

    /**
     * 根据id删除数据
     *
     * @param id id
     * @return SqlResult
     */
    public Future<SqlResult<Void>> deleteById(String id) {
        String name = this.primaryField.getAnnotation(com.run.dao.common.annotations.Column.class).name();
        EqualsTo equalsTo = new EqualsTo().withLeftExpression(new Column(name))
                .withRightExpression(new Column("#{%s}".formatted(name)));
        return delete(equalsTo, Map.of(name, id));
    }

    /**
     * 删除数据
     *
     * @param where  条件
     * @param params 参数
     * @return SqlResult
     */
    public Future<SqlResult<Void>> delete(Expression where, Map<String, Object> params, SqlClient sqlClient) {
        Delete delete = new Delete();
        delete.withTable(table);
        delete.withWhere(where);
        return delete(delete, params);
    }

    /**
     * 删除数据
     *
     * @param where  条件
     * @param params 参数
     * @return SqlResult
     */
    public Future<SqlResult<Void>> delete(Expression where, Map<String, Object> params) {
        Delete delete = new Delete();
        delete.withTable(table);
        delete.withWhere(where);
        return delete(delete, params);
    }

    /**
     * 删除数据
     *
     * @param delete 删除对象
     * @param params 参数
     * @return 异步响应
     */
    public Future<SqlResult<Void>> delete(Delete delete, Map<String, Object> params, SqlClient sqlClient) {
        return SqlTemplate.forUpdate(sqlClient, delete.toString())
                .execute(params);
    }

    /**
     * 删除数据
     *
     * @param delete 删除对象
     * @param params 参数
     * @return 异步响应
     */
    public Future<SqlResult<Void>> delete(Delete delete, Map<String, Object> params) {
        return SqlTemplate.forUpdate(client, delete.toString())
                .execute(params);
    }

    public Future<SqlResult<Void>> update(Expression where, Map<String, Object> params) {
        Update update = SqlGenUtil.generateUpdateSql(table, params, primaryField.getAnnotation(com.run.dao.common.annotations.Column.class).name());
        return SqlTemplate.forUpdate(client, update.toString())
                .execute(params);
    }

    public Future<SqlResult<Void>> update(Update update, Map<String, Object> params) {
        return SqlTemplate.forUpdate(client, update.toString())
                .execute(params);
    }

    /**
     * 根据实例对象修改数据
     *
     * @param t 实例对象
     * @return SqlResult
     */
    public Future<SqlResult<Void>> update(T t) {
        Update update = SqlGenUtil.generateUpdateSql(table, t);
        if (update.getUpdateSets().size() == 0) {
            return Future.failedFuture("不存在的数据修改");
        }
        return SqlTemplate.forUpdate(client, update.toString())
                .mapFrom(TupleMapper.mapper(T::toMap))
                .execute(t);
    }

    /**
     * 查询一条
     *
     * @param select 查询条件
     * @return 异步响应
     */
    public Future<T> one(Select select) {
        return one(select, Map.of());
    }

    /**
     * 查询一条
     *
     * @param select 查询条件
     * @param params 参数
     * @return 异步响应
     */
    public Future<T> one(Select select, Map<String, Object> params) {
        return search(select, params).compose(rowSet -> {
            int size = rowSet.size();
            if (size == 1) {
                return Future.succeededFuture(rowSet.iterator().next());
            } else if (size > 1) {
                return Future.failedFuture(new RuntimeException("数据大于1条"));
            }
            return Future.succeededFuture(null);
        });
    }

    /**
     * 查询列表
     *
     * @param where  查询条件
     * @param params 参数
     * @return 异步响应
     */
    public Future<List<T>> list(Expression where, Map<String, Object> params) {
        return _list(where, params).compose(BaseMapper::toListFuture);
    }

    /**
     * 查询列表
     *
     * @param where  查询条件
     * @param params 参数
     * @return 异步响应
     */
    public Future<RowSet<T>> _list(Expression where, Map<String, Object> params) {
        Select select = SelectUtils.buildSelectFromTable(table);
        PlainSelect plainSelect = select.getPlainSelect();
        plainSelect.withWhere(where);
        return SqlTemplate.forQuery(client, select.toString())
                .mapTo(entity::mapTo)
                .execute(params);
    }

    /**
     * 分页查询
     *
     * @param where       查询条件
     * @param currentPage 当前页
     * @param pageSize    每页大小
     * @return 分页数据
     */
    public Future<Page<T>> page(Expression where, long currentPage, long pageSize) {
        Future<Long> count = count(where);
        return _page(where, currentPage, pageSize).compose(rowSet -> count.compose(c -> {
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
     * @param where       查询条件
     * @param currentPage 当前页
     * @param pageSize    每页大小
     * @return 分页结果
     */
    private Future<RowSet<T>> _page(Expression where, long currentPage, long pageSize) {
        Select select = SelectUtils.buildSelectFromTable(table);
        select.getPlainSelect().withWhere(where);
        select.setLimit(new Limit().withOffset(new LongValue(((currentPage - 1) * pageSize))).withRowCount(new LongValue(pageSize)));
        return SqlTemplate.forQuery(client, select.toString())
                .mapTo(entity::mapTo)
                .execute(Map.of());

    }

    /**
     * 获取count
     *
     * @param where 条件
     * @return count 数
     */
    public Future<Long> count(Expression where) {
        Select select = SelectUtils.buildSelectFromTable(table);
        Function count = new Function().withName("COUNT").withParameters(new ExpressionList<>().withExpressions(new Column("*")));
        SelectItem<?> c = new SelectItem<>().withExpression(count).withAlias(new Alias("count"));
        PlainSelect plainSelect = select.getPlainSelect();
        plainSelect.withSelectItems(List.of(c)).withWhere(where);
        return SqlTemplate.forQuery(client, select.toString())
                .execute(Map.of()).compose(rows -> {
                    Row next = rows.iterator().next();
                    return Future.succeededFuture(next.getLong("count"));
                });

    }

}
