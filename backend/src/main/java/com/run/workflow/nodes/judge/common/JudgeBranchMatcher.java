package com.run.workflow.nodes.judge.common;

import com.run.workflow.WorkFlowManage;
import com.run.workflow.nodes.judge.pojo.JudgeNodeData;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/25  20:39}
 * {@code @Version 1.0}
 * {@code @注释: }
 */

public class JudgeBranchMatcher {

    private JudgeBranchMatcher() {
    }

    public static JudgeNodeData.JudgeBranch matchBranch(
            JudgeNodeData nodeData,
            WorkFlowManage workFlowManage
    ) {
        if (nodeData == null || nodeData.getBranches() == null || nodeData.getBranches().isEmpty()) {
            return null;
        }

        JudgeNodeData.JudgeBranch elseBranch = null;

        for (JudgeNodeData.JudgeBranch branch : nodeData.getBranches()) {
            if (branch == null) {
                continue;
            }

            if (JudgeNodeData.BranchType.ELSE.equals(branch.getType())) {
                elseBranch = branch;
                continue;
            }

            if (matchBranchCondition(branch, workFlowManage)) {
                return branch;
            }
        }

        return elseBranch;
    }

    private static boolean matchBranchCondition(
            JudgeNodeData.JudgeBranch branch,
            WorkFlowManage workFlowManage
    ) {
        List<JudgeNodeData.JudgeCondition> conditions = branch.getConditions();

        if (conditions == null || conditions.isEmpty()) {
            return false;
        }

        if (JudgeNodeData.BranchLogic.OR.equals(branch.getLogic())) {
            for (JudgeNodeData.JudgeCondition condition : conditions) {
                if (matchCondition(condition, workFlowManage)) {
                    return true;
                }
            }
            return false;
        }

        // 默认 AND
        for (JudgeNodeData.JudgeCondition condition : conditions) {
            if (!matchCondition(condition, workFlowManage)) {
                return false;
            }
        }

        return true;
    }

    private static boolean matchCondition(
            JudgeNodeData.JudgeCondition condition,
            WorkFlowManage workFlowManage
    ) {
        if (condition == null || condition.getCompare() == null) {
            return false;
        }

        Object leftValue = workFlowManage.getContextVariable(condition.getVariable());

        Object rightValue = null;
        if (condition.needRightValue()) {
            rightValue = resolveRightValue(condition.getValue(), workFlowManage);
        }

        return compare(leftValue, condition.getCompare(), rightValue);
    }

    /**
     * value 当前是 String：
     *
     * 普通值：
     * "你好"
     *
     * 引用变量：
     * "${start-node.question}"
     */
    private static Object resolveRightValue(
            String value,
            WorkFlowManage workFlowManage
    ) {
        if (isReferenceValue(value)) {
            List<String> path = parseReferencePath(value);
            return workFlowManage.getContextVariable(path);
        }

        return value;
    }

    private static boolean compare(
            Object leftValue,
            JudgeNodeData.CompareValue compare,
            Object rightValue
    ) {
        return switch (compare) {
            case IS_NULL -> isNullValue(leftValue);
            case IS_NOT_NULL -> !isNullValue(leftValue);

            case IS_TRUE -> isTrue(leftValue);
            case IS_NOT_TRUE -> !isTrue(leftValue);

            case EQ -> equalsValue(leftValue, rightValue);
            case NOT_EQ -> !equalsValue(leftValue, rightValue);

            case CONTAIN -> containsValue(leftValue, rightValue);
            case NOT_CONTAIN -> !containsValue(leftValue, rightValue);

            case GT -> compareNumberOrString(leftValue, rightValue) > 0;
            case GE -> compareNumberOrString(leftValue, rightValue) >= 0;
            case LT -> compareNumberOrString(leftValue, rightValue) < 0;
            case LE -> compareNumberOrString(leftValue, rightValue) <= 0;

            case LEN_EQ -> compareLength(leftValue, rightValue) == 0;
            case LEN_GE -> compareLength(leftValue, rightValue) >= 0;
            case LEN_GT -> compareLength(leftValue, rightValue) > 0;
            case LEN_LE -> compareLength(leftValue, rightValue) <= 0;
            case LEN_LT -> compareLength(leftValue, rightValue) < 0;

            case START_WITH -> toStringValue(leftValue).startsWith(toStringValue(rightValue));
            case END_WITH -> toStringValue(leftValue).endsWith(toStringValue(rightValue));

            case REGEX -> regexMatch(leftValue, rightValue);
            case WILDCARD -> wildcardMatch(leftValue, rightValue);
        };
    }

    private static boolean isReferenceValue(String value) {
        if (value == null) {
            return false;
        }

        String text = value.trim();
        return text.startsWith("${") && text.endsWith("}") && text.length() > 3;
    }

    private static List<String> parseReferencePath(String value) {
        if (!isReferenceValue(value)) {
            return List.of();
        }

        String body = value.trim().substring(2, value.trim().length() - 1).trim();

        if (body.isEmpty()) {
            return List.of();
        }

        return List.of(body.split("\\."));
    }

    private static boolean isNullValue(Object value) {
        if (value == null) {
            return true;
        }

        if (value instanceof String str) {
            return str.trim().isEmpty();
        }

        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }

        if (value instanceof Map<?, ?> map) {
            return map.isEmpty();
        }

        if (value.getClass().isArray()) {
            return Array.getLength(value) == 0;
        }

        return false;
    }

    private static boolean isTrue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }

        if (value instanceof Number number) {
            return number.doubleValue() != 0;
        }

        String text = toStringValue(value).trim();

        return "true".equalsIgnoreCase(text)
                || "1".equals(text)
                || "yes".equalsIgnoreCase(text)
                || "y".equalsIgnoreCase(text);
    }

    private static boolean equalsValue(Object leftValue, Object rightValue) {
        BigDecimal leftNumber = toBigDecimal(leftValue);
        BigDecimal rightNumber = toBigDecimal(rightValue);

        if (leftNumber != null && rightNumber != null) {
            return leftNumber.compareTo(rightNumber) == 0;
        }

        return Objects.equals(toStringValue(leftValue), toStringValue(rightValue));
    }

    private static boolean containsValue(Object leftValue, Object rightValue) {
        if (leftValue == null || rightValue == null) {
            return false;
        }

        if (leftValue instanceof Collection<?> collection) {
            return collection.stream().anyMatch(item -> equalsValue(item, rightValue));
        }

        if (leftValue.getClass().isArray()) {
            int length = Array.getLength(leftValue);
            for (int i = 0; i < length; i++) {
                Object item = Array.get(leftValue, i);
                if (equalsValue(item, rightValue)) {
                    return true;
                }
            }
            return false;
        }

        if (leftValue instanceof Map<?, ?> map) {
            return map.containsKey(rightValue) || map.containsValue(rightValue);
        }

        return toStringValue(leftValue).contains(toStringValue(rightValue));
    }

    private static int compareNumberOrString(Object leftValue, Object rightValue) {
        BigDecimal leftNumber = toBigDecimal(leftValue);
        BigDecimal rightNumber = toBigDecimal(rightValue);

        if (leftNumber != null && rightNumber != null) {
            return leftNumber.compareTo(rightNumber);
        }

        return toStringValue(leftValue).compareTo(toStringValue(rightValue));
    }

    private static int compareLength(Object leftValue, Object rightValue) {
        int length = getLength(leftValue);
        BigDecimal rightNumber = toBigDecimal(rightValue);

        if (rightNumber == null) {
            return 1;
        }

        return BigDecimal.valueOf(length).compareTo(rightNumber);
    }

    private static int getLength(Object value) {
        if (value == null) {
            return 0;
        }

        if (value instanceof CharSequence text) {
            return text.length();
        }

        if (value instanceof Collection<?> collection) {
            return collection.size();
        }

        if (value instanceof Map<?, ?> map) {
            return map.size();
        }

        if (value.getClass().isArray()) {
            return Array.getLength(value);
        }

        return toStringValue(value).length();
    }

    private static boolean regexMatch(Object leftValue, Object rightValue) {
        try {
            return Pattern
                    .compile(toStringValue(rightValue))
                    .matcher(toStringValue(leftValue))
                    .find();
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    private static boolean wildcardMatch(Object leftValue, Object rightValue) {
        String pattern = wildcardToRegex(toStringValue(rightValue));
        return Pattern.compile(pattern).matcher(toStringValue(leftValue)).matches();
    }

    private static String wildcardToRegex(String wildcard) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < wildcard.length(); i++) {
            char item = wildcard.charAt(i);

            switch (item) {
                case '*' -> builder.append(".*");
                case '?' -> builder.append(".");
                case '.', '(', ')', '[', ']', '{', '}', '+', '^', '$', '|', '\\' -> {
                    builder.append("\\").append(item);
                }
                default -> builder.append(item);
            }
        }

        return builder.toString();
    }

    private static String toStringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal decimal) {
            return decimal;
        }

        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }

        String text = String.valueOf(value).trim();

        if (text.isEmpty()) {
            return null;
        }

        try {
            return new BigDecimal(text);
        } catch (Exception e) {
            return null;
        }
    }
}