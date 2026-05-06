package com.run.workflow.common;

import com.run.workflow.WorkFlowManage;
import com.run.workflow.nodes.judge.common.JudgeBranchMatcher;
import com.run.workflow.nodes.judge.pojo.JudgeNodeData;

import java.util.List;

/**
 * 循环节点共享的条件匹配工具
 * 复用 JudgeBranchMatcher 中的条件求值逻辑
 */
public class LoopConditionMatcher {

    private LoopConditionMatcher() {
    }

    /**
     * 判断一组条件是否匹配
     *
     * @param conditions    条件列表
     * @param logic         逻辑关系（AND / OR）
     * @param workFlowManage 工作流管理器，用于获取上下文变量
     * @return 全部满足(AND)或任一满足(OR)时返回 true
     */
    public static boolean matchConditions(
            List<JudgeNodeData.JudgeCondition> conditions,
            JudgeNodeData.BranchLogic logic,
            WorkFlowManage workFlowManage
    ) {
        if (conditions == null || conditions.isEmpty()) {
            return false;
        }

        // 构造一个临时分支，复用 JudgeBranchMatcher
        JudgeNodeData.JudgeBranch branch = new JudgeNodeData.JudgeBranch();
        branch.setType(JudgeNodeData.BranchType.IF);
        branch.setLogic(logic != null ? logic : JudgeNodeData.BranchLogic.AND);
        branch.setConditions(conditions);

        // 构造临时 NodeData 包含单个分支
        JudgeNodeData nodeData = new JudgeNodeData(List.of(branch));

        JudgeNodeData.JudgeBranch matched = JudgeBranchMatcher.matchBranch(nodeData, workFlowManage);

        // 如果匹配到了 IF 分支，说明条件成立
        return matched != null && JudgeNodeData.BranchType.IF.equals(matched.getType());
    }
}
