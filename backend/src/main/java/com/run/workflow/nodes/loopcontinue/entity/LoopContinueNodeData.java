package com.run.workflow.nodes.loopcontinue.entity;

import com.run.workflow.nodes.judge.pojo.JudgeNodeData;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 循环跳过节点数据
 * 配置条件：满足时跳过当前循环迭代（等效 continue）
 */
@Getter
@Setter
public class LoopContinueNodeData {

    /**
     * 条件列表
     */
    private List<JudgeNodeData.JudgeCondition> conditions = new ArrayList<>();

    /**
     * 条件逻辑：and / or
     */
    private JudgeNodeData.BranchLogic logic = JudgeNodeData.BranchLogic.AND;
}
