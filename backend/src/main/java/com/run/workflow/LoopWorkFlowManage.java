package com.run.workflow;

import com.run.common.function.Write;

import com.run.common.util.CommonUtils;
import com.run.common.util.TemplateUtils;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.WorkFlow;
import com.run.workflow.message.struct.Content;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class LoopWorkFlowManage extends WorkFlowManage {
    @Getter
    private WorkFlowManage parentWorkFlowManage;
    @Getter
    private Function<WorkFlowManage, Map<String, Object>> loopContextCall;


    public LoopWorkFlowManage(WorkFlow workFlow, Map<String, Object> params,
                              Map<String, Map<String, Object>> context,
                              Write<WorkFlowManage, INode<?, ?>, Content, Boolean> write,
                              WorkFlowManage parentWorkflowManage,
                              Function<WorkFlowManage, Map<String, Object>> loopContextCall) {
        super(workFlow, params, context, write, wf -> wf.getNode("loop-start-node"));
        this.parentWorkFlowManage = parentWorkflowManage;
        this.loopContextCall = loopContextCall;
    }

    public LoopWorkFlowManage(WorkFlow workFlow, Map<String, Object> params,
                              Map<String, Map<String, Object>> context,
                              Write<WorkFlowManage, INode<?, ?>, Content, Boolean> write,
                              WorkFlowManage parentWorkflowManage,
                              Function<WorkFlowManage, Map<String, Object>> loopContextCall,
                              Function<WorkFlow, Node> getStartNode) {
        super(workFlow, params, context, write, getStartNode);
        this.parentWorkFlowManage = parentWorkflowManage;
        this.loopContextCall = loopContextCall;
    }

    @Override
    public String generatePrompt(String prompt) {
        String s = this.getWorkFlow().resetPrompt(prompt);
        s = this.parentWorkFlowManage.getWorkFlow().resetPrompt(s);
        Map<String, Map<String, Object>> context = this.getContext();
        return TemplateUtils.format(s, Map.of("context", context));
    }


    @Override
    public Map<String, Map<String, Object>> getContext() {
        Map<String, Map<String, Object>> context = this.getParentWorkFlowManage().getContext();
        Map<String, Map<String, Object>> selfContent = super.getContext();
        HashMap<String, Map<String, Object>> result = new HashMap<>(context);
        result.putAll(selfContent);
        return result;
    }

    public void writeLoopContext(String nodeId, String key, Object value) {
        WorkFlowManage p = getParentWorkFlowManage();
        Node node = this.getWorkFlow().getNode(nodeId);
        if (node != null) {
            this.writeContext(node.getId(), key, value);
            return;
        }
        if (p instanceof LoopWorkFlowManage lp) {
            lp.writeLoopContext(nodeId, key, value);
        } else if (p instanceof WorkFlowManage wf) {
            wf.writeContext(nodeId, key, value);
        }
    }
}
