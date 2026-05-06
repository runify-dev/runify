package com.run.workflow.nodes.loopbreak.entity;

import com.run.workflow.nodes.judge.pojo.JudgeNodeData;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 跳出循环节点数据
 * 配置条件：满足时跳出整个循环（等效 break）
 */
@Getter
@Setter
public class LoopBreakNodeData {

    /**
     * 条件列表
     */
    private List<JudgeNodeData.JudgeCondition> conditions = new ArrayList<>();

    /**
     * 条件逻辑：and / or
     */
    private JudgeNodeData.BranchLogic logic = JudgeNodeData.BranchLogic.AND;
}
