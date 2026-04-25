package com.run.workflow.nodes.judge.pojo;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/25  00:54}
 * {@code @Version 1.0}
 * {@code @注释: }
 */

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
public class JudgeNodeData {

    private List<JudgeBranch> branches = new ArrayList<>();

    public JudgeNodeData() {
    }

    public JudgeNodeData(List<JudgeBranch> branches) {
        this.branches = branches;
    }

    public static JudgeNodeData defaultData() {
        List<JudgeBranch> branches = new ArrayList<>();
        branches.add(JudgeBranch.ifBranch());
        branches.add(JudgeBranch.elseBranch());
        return new JudgeNodeData(branches);
    }

    @Getter
    @Setter
    public static class JudgeBranch {

        private String id;
        private BranchType type;
        private BranchLogic logic;
        private List<JudgeCondition> conditions = new ArrayList<>();

        public JudgeBranch() {
        }

        public JudgeBranch(String id, BranchType type, BranchLogic logic, List<JudgeCondition> conditions) {
            this.id = id;
            this.type = type;
            this.logic = logic;
            this.conditions = conditions;
        }

        public static JudgeBranch ifBranch() {
            JudgeBranch branch = new JudgeBranch();
            branch.setId(UUID.randomUUID().toString());
            branch.setType(BranchType.IF);
            branch.setLogic(BranchLogic.AND);
            branch.setConditions(List.of(JudgeCondition.empty()));
            return branch;
        }

        public static JudgeBranch elseifBranch() {
            JudgeBranch branch = new JudgeBranch();
            branch.setId(UUID.randomUUID().toString());
            branch.setType(BranchType.ELSEIF);
            branch.setLogic(BranchLogic.AND);
            branch.setConditions(List.of(JudgeCondition.empty()));
            return branch;
        }

        public static JudgeBranch elseBranch() {
            JudgeBranch branch = new JudgeBranch();
            branch.setId(UUID.randomUUID().toString());
            branch.setType(BranchType.ELSE);
            branch.setLogic(null);
            branch.setConditions(new ArrayList<>());
            return branch;
        }

        public boolean isElse() {
            return BranchType.ELSE.equals(type);
        }

        public boolean isAnd() {
            return logic == null || BranchLogic.AND.equals(logic);
        }

        public boolean isOr() {
            return BranchLogic.OR.equals(logic);
        }
    }

    @Getter
    @Setter
    public static class JudgeCondition {

        private String id;

        /**
         * 左值变量路径
         * 例如：["start-node", "question"]
         */
        private List<String> variable = new ArrayList<>();

        /**
         * 比较符
         * 例如：eq / contain / is_null
         */
        private CompareValue compare;

        /**
         * 右值
         *
         * 普通字符串：
         * "你好"
         *
         * 变量引用：
         * "${start-node.question}"
         */
        private String value;

        public JudgeCondition() {
        }

        public JudgeCondition(String id, List<String> variable, CompareValue compare, String value) {
            this.id = id;
            this.variable = variable;
            this.compare = compare;
            this.value = value;
        }

        public static JudgeCondition empty() {
            JudgeCondition condition = new JudgeCondition();
            condition.setId(UUID.randomUUID().toString());
            condition.setVariable(new ArrayList<>());
            condition.setCompare(null);
            condition.setValue("");
            return condition;
        }

        public boolean needRightValue() {
            if (compare == null) {
                return true;
            }

            return !List.of(
                    CompareValue.IS_NULL,
                    CompareValue.IS_NOT_NULL,
                    CompareValue.IS_TRUE,
                    CompareValue.IS_NOT_TRUE
            ).contains(compare);
        }

        public String getVariableExpression() {
            if (variable == null || variable.isEmpty()) {
                return "";
            }

            return String.join(".", variable);
        }

        public boolean isValueReference() {
            return value != null && value.matches("^\\$\\{.+}$");
        }

        public List<String> getValueReferencePath() {
            if (!isValueReference()) {
                return List.of();
            }

            String body = value.substring(2, value.length() - 1);

            if (body.isBlank()) {
                return List.of();
            }

            return List.of(body.split("\\."));
        }
    }

    public enum BranchType {
        IF("if"),
        ELSEIF("elseif"),
        ELSE("else");

        private final String value;

        BranchType(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @JsonCreator
        public static BranchType fromValue(String value) {
            if (value == null) {
                return null;
            }

            for (BranchType type : values()) {
                if (Objects.equals(type.value, value)) {
                    return type;
                }
            }

            throw new IllegalArgumentException("Unknown branch type: " + value);
        }
    }

    public enum BranchLogic {
        AND("and"),
        OR("or");

        private final String value;

        BranchLogic(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @JsonCreator
        public static BranchLogic fromValue(String value) {
            if (value == null) {
                return null;
            }

            for (BranchLogic logic : values()) {
                if (Objects.equals(logic.value, value)) {
                    return logic;
                }
            }

            throw new IllegalArgumentException("Unknown branch logic: " + value);
        }
    }

    public enum CompareValue {
        IS_NULL("is_null"),
        IS_NOT_NULL("is_not_null"),
        CONTAIN("contain"),
        NOT_CONTAIN("not_contain"),
        EQ("eq"),
        NOT_EQ("not_eq"),
        GE("ge"),
        GT("gt"),
        LE("le"),
        LT("lt"),
        LEN_EQ("len_eq"),
        LEN_GE("len_ge"),
        LEN_GT("len_gt"),
        LEN_LE("len_le"),
        LEN_LT("len_lt"),
        IS_TRUE("is_true"),
        IS_NOT_TRUE("is_not_true"),
        START_WITH("start_with"),
        END_WITH("end_with"),
        REGEX("regex"),
        WILDCARD("wildcard");

        private final String value;

        CompareValue(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @JsonCreator
        public static CompareValue fromValue(String value) {
            if (value == null) {
                return null;
            }

            for (CompareValue compareValue : values()) {
                if (Objects.equals(compareValue.value, value)) {
                    return compareValue;
                }
            }

            throw new IllegalArgumentException("Unknown compare value: " + value);
        }
    }
}