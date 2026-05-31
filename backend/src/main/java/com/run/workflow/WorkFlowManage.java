package com.run.workflow;

import com.run.common.function.Write;
import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.util.CommonUtils;
import com.run.common.util.TemplateUtils;
import com.run.workflow.entity.*;
import com.run.workflow.message.aggregator.AggregationManager;
import com.run.workflow.message.struct.Content;
import com.run.workflow.message.struct.FailureContent;
import com.run.workflow.message.struct.Position;
import com.run.workflow.nodes.NodeManage;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/19  21:15}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Slf4j
public class WorkFlowManage {
    /**
     * 工作流对象,存储了工作流相关信息
     */
    @Getter
    private final WorkFlow workFlow;
    /**
     * 用户收集参数
     */
    @Getter
    private final Map<String, Object> params;
    private boolean done = false;
    /**
     * 运行开始时间
     */
    private final LocalDateTime startTime;

    private final AggregationManager aggregationManager = new AggregationManager();

    /**
     * 用于存储上下文
     */
    @Getter
    private final Map<String, Map<String, Object>> context;
    /**
     * 获取开始执行节点,正常情况下开始节点都是Start,但是在存在中断节点需要召回时,就是中断节点
     */
    private final Function<WorkFlow, Node> getStartNode;
    /**
     * 校验器,用于校验对象值是否正确
     */
    private final static Validator validator;

    static {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }


    /**
     * 节点实例化
     */
    private Function<NewNodeParamsInstance, INode<?, ?>> nodeNewInstance;
    /**
     * 工作流写出
     */
    @Getter
    private final Write<WorkFlowManage, INode<?, ?>, Content, Boolean> write;
    /**
     * 已运行的节点信息
     */
    @Getter
    private List<INode<?, ?>> nodes;

    /**
     * 工作流是否已取消
     */
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public WorkFlowManage(WorkFlow workFlow,
                          Map<String, Object> params,
                          Map<String, Map<String, Object>> context,
                          Write<WorkFlowManage, INode<?, ?>, Content, Boolean> write,
                          Function<WorkFlow, Node> getStartNode) {
        this.workFlow = workFlow;
        this.getStartNode = getStartNode;
        this.nodeNewInstance = NodeManage.of();
        this.write = write;
        this.params = params;
        this.context = context;
        this.nodes = new ArrayList<>();
        this.startTime = LocalDateTime.now();
    }

    public WorkFlowManage(WorkFlow workFlow,
                          Map<String, Object> params,
                          Map<String, Map<String, Object>> context,
                          Write<WorkFlowManage, INode<?, ?>, Content, Boolean> write) {
        this(workFlow, params, context, write, w -> w.getNode("start-node"));
    }

    public void invoke() {
        Node startNode = this.getStartNode.apply(workFlow);
        invoke(startNode, null);
    }

    public void asyncInvoke(Node node, INode<?, ?> upINode) {
        Thread.ofVirtual().start(() -> invoke(node, upINode));
    }

    /**
     * 执行下一个节点
     *
     * @param upINode     上一个节点信息
     * @param getNextNode 获取下一个节点的函数
     */
    public void nextInvoke(INode<?, ?> upINode, Supplier<List<Node>> getNextNode) {
        if (this.cancelled.get()) this.assertionEnd();
        try {
            List<Node> nodes = getNextNode.get();
            if (nodes.size() == 1) {
                Thread.ofVirtual().start(() -> invoke(nodes.getFirst(), upINode));
            } else if (nodes.size() > 1) {
                for (Node node : nodes) {
                    Thread.ofVirtual().start(() -> invoke(node, upINode));
                }
            } else {
                // 如果没有下一个运行节点,那么就判断是否所有正在运行的节点都执行结束,如果都执行结束那么工作流就结束
                this.assertionEnd();
            }
        } catch (Exception e) {
            write(upINode, new FailureContent(e.toString(),
                    upINode,
                    (String) this.getParams().get("workflowRunId"),
                    CommonUtils.uuid7().toString()));
            upINode.status = NodeStatus.FAIL;
            this.assertionEnd();
        }
    }

    public void nextFailInvoke(INode<?, ?> upINode, Throwable throwable) {
        String id = upINode.node.getId();
        List<DefaultKeyValue<Edge, Node>> nextList = getNextList(id);
        Boolean errorCaptureEnabled = upINode.node.getProperties().getBoolean("errorCaptureEnabled");
        Supplier<List<Node>> handle;
        if (errorCaptureEnabled != null && errorCaptureEnabled) {
            handle = () -> nextList.stream().filter(edgeNodeDefaultKeyValue -> {
                        Edge edge = edgeNodeDefaultKeyValue.getKey();
                        return Strings.CS.equals(edge.getSourceNodeId() + "_right" + "_main" + "_fail", edge.getString("sourceAnchorId"));
                    })
                    .map(DefaultKeyValue::getValue)
                    .toList();
            nextInvoke(upINode, handle);
        } else {
            write(upINode, new FailureContent(throwable.getMessage(), upINode, (String) this.params.get("workflowRunId"), UUID.randomUUID().toString()));
            this.assertionEnd();
        }
    }

    /**
     * 执行节点
     *
     * @param node    需要执行的节点数据信息
     * @param upINode 上一个节点执行器
     */
    @SneakyThrows
    public void invoke(Node node, INode<?, ?> upINode) {
        if (this.cancelled.get()) return;
        List<String> upNodeIdList = List.of();
        if (upINode != null) {
            upNodeIdList = new ArrayList<>(upINode.getUpNodeIdList());
            upNodeIdList.add(upINode.node.getId());
        }
        NewNodeParamsInstance instance = NewNodeParamsInstance.of(node, new JsonObject(params), upNodeIdList, this.validator, upINode, workFlow.getWorkflowType());
        INode<?, ?> iNode = this.nodeNewInstance.apply(instance);
        try {
            // 添加方便管理
            this.nodes.add(iNode);
            // 执行节点
            NodeResult<?> invoke = iNode.invoke();
            Supplier<List<Node>> handle = invoke.handle(this);
            if (handle != null) {
                nextInvoke(iNode, handle);
            } else {
                this.assertionEnd();
            }
        } catch (Throwable e) {
            log.error("执行工作流中发生异常:", e);
            iNode.status = NodeStatus.FAIL;
            write(iNode, new FailureContent(e.toString(), iNode, (String) this.getParams().get("workflowRunId"), CommonUtils.uuid7().toString()));
            this.assertionEnd();
        }

    }

    /**
     * 写入上下文
     *
     * @param iNode 当前节点
     * @param key   需要写入数据的key
     * @param value 需要写入数据的值
     */
    public void writeContext(INode<?, ?> iNode, String key, Object value) {
        Map<String, Object> m = this.context.computeIfAbsent(iNode.node.getId(), k -> new HashMap<>());
        m.put(key, value);
        iNode.context.put(key, value);
    }

    /**
     * 写入上下文
     *
     * @param nodeId 节点id
     * @param key    需要写入数据的key
     * @param value  需要写入数据的值
     */
    public void writeContext(String nodeId, String key, Object value) {
        Map<String, Object> m = this.context.computeIfAbsent(nodeId, k -> new HashMap<>());
        m.put(key, value);
    }

    public void writeGlobalContext(String key, Object value) {
        Map<String, Object> m = this.context.computeIfAbsent("global", k -> new HashMap<>());
        m.put(key, value);
    }

    /**
     * 将节点数据输出出去
     *
     * @param chunk 需要输出的chunk
     */
    public void write(INode<?, ?> node, Content chunk) {
        aggregationManager.aggregate(chunk);
        this.write.write(this, node, chunk, false);
    }

    public void writeChildren(INode<?, ?> node, Content chunk, Integer currentIndex) {
        String id = node.node.getId();
        Position position = chunk.getPosition();
        chunk.setPosition(new Position(id, currentIndex, position));
        aggregationManager.aggregate(chunk);
        this.write.write(this, node, chunk, false);
    }

    public void end() {
        this.write.write(this, null, null, true);
    }

    /**
     * 取消整个工作流
     * 取消所有正在运行的节点
     */
    public void cancel() {
        this.cancelled.set(true);
        for (INode<?, ?> node : this.nodes) {
            node.cancel();
        }

    }

    /**
     * 工作流是否已取消
     */
    public boolean isCancelled() {
        return this.cancelled.get();
    }

    public void assertionEnd() {
        List<NodeStatus> running = List.of(NodeStatus.RUNNING, NodeStatus.BEFORE_RUNNING);
        boolean b = this.nodes.stream().anyMatch(iNode -> running.contains(iNode.status));
        if (!b && !done) {
            this.done = true;
            this.write.write(this, null, null, true);
        }
    }

    /**
     * 获取上下文的数据
     *
     * @param contextRef 上下文地址
     * @return 上下文数据
     */
    @SuppressWarnings("all")
    public Object getContextVariable(List<String> contextRef) {
        Map<String, ?> context = this.getContext();
        for (int i = 0; i < contextRef.size(); i++) {
            String key = contextRef.get(i);
            if (i == contextRef.size() - 1) {
                return context.get(key);
            } else {
                Object o = context.get(key);
                if (o instanceof JsonObject jo) {
                    context = jo.getMap();
                } else if (o instanceof Map<?, ?>) {
                    context = (Map<String, ?>) o;
                } else {
                    return null;
                }
            }
        }
        return null;
    }


    public long getRuntime() {
        long executionTime = ChronoUnit.MILLIS.between(this.startTime, LocalDateTime.now());
        return executionTime / 1000;
    }

    /**
     * 生成提示词
     *
     * @param prompt 提示词数据
     * @return 解析后的提示词
     */
    public String generatePrompt(String prompt) {
        String s = this.workFlow.resetPrompt(prompt);
        return TemplateUtils.format(s, Map.of("context", this.context));
    }

    public List<Content> getChunks() {
        return aggregationManager.getContents();
    }

    /**
     * 获取下一个节点列表
     *
     * @param node_id 节点id
     * @return 节点列表 Edge代表当前Node的连线
     */
    public List<DefaultKeyValue<Edge, Node>> getNextList(String node_id) {
        return workFlow.getNextNodes(node_id);
    }

    public Supplier<List<Node>> nextNodeSupplier(String nodeId) {
        return () -> this.getNextList(nodeId).stream().filter(edgeNodeDefaultKeyValue -> {
            Edge edge = edgeNodeDefaultKeyValue.getKey();
            return Strings.CS.equals(edge.getSourceNodeId() + "_right" + "_main" + "_success", edge.getString("sourceAnchorId"));
        }).map(DefaultKeyValue::getValue).toList();
    }

    public Supplier<List<Node>> nextCancelNodeSupplier() {
        return null;
    }

    /**
     * 错误的返回值
     *
     * @param nodeId              节点Id
     * @param errorCaptureEnabled 是否捕获异常
     * @return 下一个执行节点
     */
    public Supplier<List<Node>> nextFailNodeSupplier(String nodeId, Boolean errorCaptureEnabled) {
        if (errorCaptureEnabled) {
            List<DefaultKeyValue<Edge, Node>> nextList = getNextList(nodeId);
            return () -> nextList.stream().filter(edgeNodeDefaultKeyValue -> {
                        Edge edge = edgeNodeDefaultKeyValue.getKey();
                        return Strings.CS.equals(edge.getSourceNodeId() + "_right" + "_main" + "_fail", edge.getString("sourceAnchorId"));
                    })
                    .map(DefaultKeyValue::getValue)
                    .toList();

        } else {
            return List::of;
        }
    }

    public int getPromptTokens() {
        return nodes.stream().map(INode::getPromptTokens).mapToInt(Integer::intValue).sum();
    }

    public int getCompletionTokens() {
        return nodes.stream().map(INode::getCompletionTokens).mapToInt(Integer::intValue).sum();
    }

    public Path getWorkingDirectory() {
        UUID conversationId = (UUID) this.getParams().getOrDefault("conversationId", CommonUtils.uuid7());
        return Path.of(System.getProperty("user.home") + "/.runify/" + conversationId);
    }

    public void clear() {
        this.context.clear();
        this.nodes.clear();
        this.aggregationManager.clear();
    }

}
