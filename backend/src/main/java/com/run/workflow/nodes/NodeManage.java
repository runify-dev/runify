package com.run.workflow.nodes;

import com.run.common.util.ClassScanUtil;
import com.run.workflow.INode;
import com.run.workflow.WorkflowType;
import com.run.workflow.entity.NewNodeParamsInstance;
import com.run.workflow.entity.Node;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/19  22:59}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class NodeManage implements Function<NewNodeParamsInstance, INode<?, ?>> {
    private ConcurrentHashMap<WorkflowType, ConcurrentMap<String, Class<? extends INode>>> nodeInstanceMap;

    public static NodeManage of() {
        List<Class<? extends INode>> classList = ClassScanUtil.getClassList("com.run.workflow.nodes", INode.class);
        return new NodeManage(classList);
    }

    public static NodeManage of(String packageName) {
        List<Class<? extends INode>> classList = ClassScanUtil.getClassList(packageName, INode.class);
        return new NodeManage(classList);
    }

    @SneakyThrows
    public NodeManage(List<Class<? extends INode>> nodeInstanceList) {
        ConcurrentHashMap<WorkflowType, ConcurrentMap<String, Class<? extends INode>>> result = new ConcurrentHashMap<>();
        for (Class<? extends INode> iNodeClass : nodeInstanceList) {
            Field field = FieldUtils.getDeclaredField(iNodeClass, "type");
            Field supportWorkflowField = FieldUtils.getDeclaredField(iNodeClass, "supportWorkflow");
            field.setAccessible(true);
            String nodeType = (String) field.get(null);
            supportWorkflowField.setAccessible(true);
            List<WorkflowType> supportWorkflow = (List<WorkflowType>) supportWorkflowField.get(null);
            for (WorkflowType workflowType : supportWorkflow) {
                ConcurrentMap<String, Class<? extends INode>> inner = result.computeIfAbsent(workflowType, t -> new ConcurrentHashMap<>());
                inner.put(nodeType, iNodeClass);
            }
        }
        this.nodeInstanceMap = result;
    }


    @Override
    public INode<?, ?> apply(NewNodeParamsInstance newNodeParamsInstance) {
        /***
         * Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator
         */
        if (!this.nodeInstanceMap.contains(newNodeParamsInstance.getWorkflowType()) &&   !this.nodeInstanceMap.get(newNodeParamsInstance.getWorkflowType()).containsKey(newNodeParamsInstance.getNode().getType())) {
            throw new RuntimeException("工作流不支持当前节点掉用");
        }
        Class<? extends INode> aClass = this.nodeInstanceMap.get(newNodeParamsInstance.getWorkflowType()).get(newNodeParamsInstance.getNode().getType());
        try {
            Constructor<? extends INode> constructor = aClass.getConstructor(new Class[]{Node.class, JsonObject.class, List.class, String.class, JsonObject.class, Validator.class, INode.class});
            INode<?, ?> iNode = constructor.newInstance(newNodeParamsInstance.getNode(),
                    newNodeParamsInstance.getParams(),
                    newNodeParamsInstance.getUpNodeIdList(),
                    newNodeParamsInstance.getSalt(),
                    newNodeParamsInstance.getContext(),
                    newNodeParamsInstance.getValidator(),
                    newNodeParamsInstance.getUpNode());
            return iNode;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

    }
}
