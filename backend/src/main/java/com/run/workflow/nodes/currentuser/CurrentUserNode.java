package com.run.workflow.nodes.currentuser;

import com.run.RunApplication;
import com.run.common.keyvalue.DefaultKeyValue;
import com.run.dao.mapper.DatasourceMapper;
import com.run.datasources.DataSourceManage;
import com.run.datasources.SimpleCache;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.nodes.currentuser.pojo.CurrentUserNodeData;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * 当前用户节点
 * 取请求里的凭证 -> 查会话缓存拿到用户;角色段/权限段按需开启,
 * 可从用户对象内抽取(inline)或按 userId 查独立缓存(cache,支持单独更新)。
 * 节点本身不做拦截,是否 401/403 由下游 judge + 响应节点决定。
 */
@Slf4j
public class CurrentUserNode extends INode<CurrentUserNode, CurrentUserNodeData> {

    public final static String type = "current-user-node";

    public final static List<WorkflowType> supportWorkflow = List.of(WorkflowType.PROCESSOR_HTTP);

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public CurrentUserNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public CurrentUserNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    @Override
    public void cancel() {
        super.cancel();
        cancelled.set(true);
    }

    public static class Handle implements BiFunction<WorkFlowManage, CurrentUserNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, CurrentUserNode node) {
            CurrentUserNodeData data = node.params;
            DatasourceMapper mapper = RunApplication.appComponent.dataSourceMapper();
            Vertx vertx = RunApplication.appComponent.vertx();

            String credential = resolveCredential(data, workFlowManage);
            if (StringUtils.isBlank(credential)) {
                // 请求里没带凭证,视为未登录,不查缓存
                writeAnonymous(workFlowManage, node);
                node.status = NodeStatus.SUCCESS;
                return nextSupplier(workFlowManage, node);
            }

            String sessionKey = StringUtils.defaultString(data.getKeyPrefix()) + credential;
            DataSourceManage.getCacheAsync(UUID.fromString(data.getSessionCacheId()), (uuid, dm) -> dm.getById(uuid.toString()), mapper, vertx)
                    .compose(cache -> Future.fromCompletionStage(cache.get(sessionKey, Object.class).toCompletableFuture()))
                    .onSuccess(optional -> {
                        if (node.cancelled.get()) {
                            workFlowManage.nextInvoke(node, workFlowManage.nextCancelNodeSupplier());
                            return;
                        }
                        // 回调内抛异常不会被 WorkFlowManage.invoke 的 try/catch 兜住,
                        // 逃逸会让节点永不结束(HTTP 请求挂起),这里统一兜底转为节点失败
                        try {
                            Object user = optional.orElse(null);
                            if (user == null) {
                                // 会话缓存未命中 = 未登录,后续段不再查
                                writeAnonymous(workFlowManage, node);
                                node.status = NodeStatus.SUCCESS;
                                workFlowManage.nextInvoke(node, nextSupplier(workFlowManage, node));
                                return;
                            }
                            String userId = asString(getField(user, StringUtils.defaultIfBlank(data.getUserIdField(), "id")));
                            Future<Object> rolesFuture = resolveSegment(data.getRoles(), user, userId, mapper, vertx);
                            Future<Object> permissionsFuture = resolveSegment(data.getPermissions(), user, userId, mapper, vertx);
                            Future.all(rolesFuture, permissionsFuture)
                                    .onComplete(ar -> {
                                        if (node.cancelled.get()) {
                                            workFlowManage.nextInvoke(node, workFlowManage.nextCancelNodeSupplier());
                                            return;
                                        }
                                        workFlowManage.writeContext(node, "authenticated", true);
                                        workFlowManage.writeContext(node, "user", user);
                                        workFlowManage.writeContext(node, "roles", rolesFuture.succeeded() ? rolesFuture.result() : null);
                                        workFlowManage.writeContext(node, "permissions", permissionsFuture.succeeded() ? permissionsFuture.result() : null);
                                        node.status = NodeStatus.SUCCESS;
                                        workFlowManage.nextInvoke(node, nextSupplier(workFlowManage, node));
                                    });
                        } catch (Throwable e) {
                            workFlowManage.nextInvoke(node, node.handleFail(workFlowManage, e));
                        }
                    })
                    .onFailure(e -> {
                        if (node.cancelled.get()) {
                            workFlowManage.nextInvoke(node, workFlowManage.nextCancelNodeSupplier());
                            return;
                        }
                        workFlowManage.nextInvoke(node, node.handleFail(workFlowManage, e));
                    });

            return null;
        }

        private static Supplier<List<Node>> nextSupplier(WorkFlowManage workFlowManage, CurrentUserNode node) {
            return () -> workFlowManage
                    .getNextList(node.node.getId())
                    .stream()
                    .map(DefaultKeyValue::getValue)
                    .toList();
        }

        /**
         * 未登录:authenticated=false,其余给空
         */
        private static void writeAnonymous(WorkFlowManage workFlowManage, CurrentUserNode node) {
            workFlowManage.writeContext(node, "authenticated", false);
            workFlowManage.writeContext(node, "user", null);
            workFlowManage.writeContext(node, "roles", null);
            workFlowManage.writeContext(node, "permissions", null);
        }

        /**
         * 按配置从请求里取凭证:header / cookie / query,并剥离前缀
         */
        private static String resolveCredential(CurrentUserNodeData data, WorkFlowManage workFlowManage) {
            RoutingContext routingContext = (RoutingContext) workFlowManage.getParams().get("context");
            if (routingContext == null) {
                return null;
            }
            String field = data.getCredentialField();
            if (StringUtils.isBlank(field)) {
                return null;
            }
            String location = StringUtils.defaultIfBlank(data.getCredentialLocation(), "header");
            String raw;
            if (Strings.CS.equals(location, "cookie")) {
                io.vertx.core.http.Cookie cookie = routingContext.request().getCookie(field);
                raw = cookie == null ? null : cookie.getValue();
            } else if (Strings.CS.equals(location, "query")) {
                raw = routingContext.queryParams().get(field);
            } else {
                raw = routingContext.request().getHeader(field);
            }
            if (StringUtils.isBlank(raw)) {
                return null;
            }
            raw = raw.trim();
            String prefix = data.getCredentialPrefix();
            if (StringUtils.isNotEmpty(prefix) && Strings.CS.startsWith(raw, prefix)) {
                raw = raw.substring(prefix.length()).trim();
            }
            return raw;
        }

        /**
         * 解析角色段/权限段:未开启返回 null;inline 从用户对象抽字段;cache 按 userId 查独立缓存。
         * 段内失败降级为 null,不让整个节点失败。
         */
        private static Future<Object> resolveSegment(CurrentUserNodeData.RefSegment segment,
                                                     Object user,
                                                     String userId,
                                                     DatasourceMapper mapper,
                                                     Vertx vertx) {
            if (segment == null || !Boolean.TRUE.equals(segment.getEnabled())) {
                return Future.succeededFuture(null);
            }
            if (Strings.CS.equals(segment.getSource(), "cache")) {
                if (StringUtils.isBlank(segment.getCacheId()) || StringUtils.isBlank(userId)) {
                    return Future.succeededFuture(null);
                }
                String key = StringUtils.defaultString(segment.getKeyPrefix()) + userId;
                Promise<Object> promise = Promise.promise();
                DataSourceManage.getCacheAsync(UUID.fromString(segment.getCacheId()), (uuid, dm) -> dm.getById(uuid.toString()), mapper, vertx)
                        .compose(cache -> Future.<java.util.Optional<Object>>fromCompletionStage(cache.get(key, Object.class).toCompletableFuture()))
                        .onSuccess(optional -> {
                            Object value = optional.orElse(null);
                            promise.complete(value == null ? null : extractValueField(value, segment.getValueField()));
                        })
                        .onFailure(e -> {
                            log.warn("当前用户节点读取缓存失败,该段降级为 null: {}", e.getMessage());
                            promise.complete(null);
                        });
                return promise.future();
            }
            // inline:从用户对象里抽字段
            if (StringUtils.isBlank(segment.getField())) {
                return Future.succeededFuture(null);
            }
            return Future.succeededFuture(getField(user, segment.getField()));
        }

        private static Object extractValueField(Object value, String valueField) {
            if (StringUtils.isBlank(valueField)) {
                return value;
            }
            return getField(value, valueField);
        }

        /**
         * 从 JsonObject / Map 里取字段,取不到返回 null
         */
        private static Object getField(Object source, String field) {
            if (source == null || StringUtils.isBlank(field)) {
                return null;
            }
            if (source instanceof JsonObject jo) {
                return jo.getValue(field);
            }
            if (source instanceof Map<?, ?> map) {
                return map.get(field);
            }
            return null;
        }

        private static String asString(Object value) {
            return value == null ? null : value.toString();
        }
    }

    @Override
    public CurrentUserNodeData getNodeData(JsonObject params) {
        return node.getProperties().getJsonObject("nodeData").mapTo(CurrentUserNodeData.class);
    }

    @Override
    public NodeResult<CurrentUserNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
