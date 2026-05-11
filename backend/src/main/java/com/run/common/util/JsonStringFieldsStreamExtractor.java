package com.run.common.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class JsonStringFieldsStreamExtractor {

    private enum State {
        OUTSIDE,
        IN_KEY,
        IN_SKIP_STRING,
        IN_TARGET_STRING
    }

    private final Set<String> targetFields;
    private final boolean extractAllStringFields;

    private State state = State.OUTSIDE;

    private int objectDepth = 0;
    private int arrayDepth = 0;

    private boolean expectingKey = false;
    private boolean expectingColon = false;
    private boolean expectingValue = false;

    private String lastKey;
    private String currentValueField;

    private final StringBuilder keyBuffer = new StringBuilder();

    private boolean keyEscape = false;
    private int keyUnicodeRemaining = 0;
    private int keyUnicodeValue = 0;

    private boolean skipEscape = false;

    private boolean targetEscape = false;
    private int targetUnicodeRemaining = 0;
    private int targetUnicodeValue = 0;

    /**
     * 用于处理 \uD83D\uDE00 这种 unicode 代理对。
     * <p>
     * 如果高代理项已经出现，但低代理项还没到，先缓存起来，
     * 避免把半个 emoji 直接推给前端。
     */
    private char pendingTargetHighSurrogate = 0;

    /**
     * 默认提取所有顶层字符串字段。
     */
    public JsonStringFieldsStreamExtractor() {
        this.targetFields = Set.of();
        this.extractAllStringFields = true;
    }

    /**
     * 只提取指定字段。
     * <p>
     * 例如：
     * new JsonStringFieldsStreamExtractor(Set.of("code", "content", "markdown"))
     */
    public JsonStringFieldsStreamExtractor(Set<String> targetFields) {
        this.targetFields = targetFields == null ? Set.of() : Set.copyOf(targetFields);
        this.extractAllStringFields = this.targetFields.isEmpty();
    }

    /**
     * 输入 arguments 增量，返回本次新增的字段内容。
     */
    public Map<String, String> feed(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return Map.of();
        }

        Map<String, StringBuilder> builders = new LinkedHashMap<>();

        for (int i = 0; i < chunk.length(); i++) {
            char c = chunk.charAt(i);

            switch (state) {
                case OUTSIDE -> handleOutside(c);
                case IN_KEY -> handleKey(c);
                case IN_SKIP_STRING -> handleSkipString(c);
                case IN_TARGET_STRING -> handleTargetString(c, builders);
            }
        }

        if (builders.isEmpty()) {
            return Map.of();
        }

        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, StringBuilder> entry : builders.entrySet()) {
            result.put(entry.getKey(), entry.getValue().toString());
        }

        return result;
    }

    public void reset() {
        state = State.OUTSIDE;

        objectDepth = 0;
        arrayDepth = 0;

        expectingKey = false;
        expectingColon = false;
        expectingValue = false;

        lastKey = null;
        currentValueField = null;

        keyBuffer.setLength(0);

        keyEscape = false;
        keyUnicodeRemaining = 0;
        keyUnicodeValue = 0;

        skipEscape = false;

        targetEscape = false;
        targetUnicodeRemaining = 0;
        targetUnicodeValue = 0;
        pendingTargetHighSurrogate = 0;
    }

    public boolean isExtracting() {
        return state == State.IN_TARGET_STRING;
    }

    public String currentValueField() {
        return currentValueField;
    }

    private void handleOutside(char c) {
        if (expectingColon) {
            if (Character.isWhitespace(c)) {
                return;
            }

            if (c == ':') {
                expectingColon = false;
                expectingValue = true;
                return;
            }

            // 非法 JSON 容错：不是冒号就放弃当前 key，并且不让该字符被 processStructural 误处理。
            expectingColon = false;
            lastKey = null;
            return;
        }

        if (expectingValue) {
            if (Character.isWhitespace(c)) {
                return;
            }

            expectingValue = false;

            boolean shouldExtract = shouldExtractField(lastKey);
            String fieldForExtract = lastKey;

            // 不论是字符串值还是其他类型的值，到这里 lastKey 都用完了，立即清空，避免脏状态残留。
            lastKey = null;

            if (c == '"') {
                if (shouldExtract) {
                    startTargetString(fieldForExtract);
                } else {
                    startSkipString();
                }
                return;
            }

            // 非字符串值（对象、数组、数字、布尔、null），交给结构处理。
            processStructural(c);
            return;
        }

        processStructural(c);
    }

    private void processStructural(char c) {
        /*
         * 关键点：
         * 只要在 OUTSIDE 状态遇到字符串开头，就必须进入字符串跳过状态。
         *
         * 否则嵌套对象/数组里的字符串内容如果包含 { } [ ]，
         * 会被错误地当成 JSON 结构字符，导致 depth 错乱。
         */
        if (c == '"') {
            if (isInTopObject() && expectingKey) {
                startKey();
            } else {
                startSkipString();
            }
            return;
        }

        if (c == '{') {
            objectDepth++;

            if (isInTopObject()) {
                // 刚进入顶层对象，等待第一个 key。
                expectingKey = true;
                expectingColon = false;
                expectingValue = false;
                lastKey = null;
            }

            return;
        }

        if (c == '}') {
            if (objectDepth > 0) {
                objectDepth--;
            }

            if (objectDepth == 0) {
                // 顶层对象结束，彻底清空状态。
                expectingKey = false;
                expectingColon = false;
                expectingValue = false;
                lastKey = null;
            } else if (isInTopObject()) {
                // 从嵌套对象返回顶层对象，重置等待状态。
                // 顶层此时刚结束一个 value，下一个有意义的字符应当是 , 或 }。
                expectingKey = false;
                expectingColon = false;
                expectingValue = false;
                lastKey = null;
            }

            return;
        }

        if (c == '[') {
            arrayDepth++;
            return;
        }

        if (c == ']') {
            if (arrayDepth > 0) {
                arrayDepth--;
            }

            if (isInTopObject()) {
                // 顶层 value 是数组，刚结束。下一个有意义的字符应当是 , 或 }。
                expectingKey = false;
                expectingColon = false;
                expectingValue = false;
                lastKey = null;
            }

            return;
        }

        if (isInTopObject() && c == ',') {
            expectingKey = true;
            expectingColon = false;
            expectingValue = false;
            lastKey = null;
        }
    }

    private boolean isInTopObject() {
        return objectDepth == 1 && arrayDepth == 0;
    }

    private boolean shouldExtractField(String fieldName) {
        if (fieldName == null) {
            return false;
        }

        return extractAllStringFields || targetFields.contains(fieldName);
    }

    private void startKey() {
        keyBuffer.setLength(0);

        keyEscape = false;
        keyUnicodeRemaining = 0;
        keyUnicodeValue = 0;

        expectingKey = false;
        state = State.IN_KEY;
    }

    private void startSkipString() {
        skipEscape = false;
        state = State.IN_SKIP_STRING;
    }

    private void startTargetString(String fieldName) {
        currentValueField = fieldName;

        targetEscape = false;
        targetUnicodeRemaining = 0;
        targetUnicodeValue = 0;
        pendingTargetHighSurrogate = 0;

        state = State.IN_TARGET_STRING;
    }

    private void handleKey(char c) {
        if (keyUnicodeRemaining > 0) {
            int hex = Character.digit(c, 16);

            if (hex >= 0) {
                keyUnicodeValue = (keyUnicodeValue << 4) + hex;
                keyUnicodeRemaining--;

                if (keyUnicodeRemaining == 0) {
                    keyBuffer.append((char) keyUnicodeValue);
                }
            } else {
                // 非法 unicode 转义容错。
                keyUnicodeRemaining = 0;
                keyUnicodeValue = 0;
                keyBuffer.append(c);
            }

            return;
        }

        if (keyEscape) {
            switch (c) {
                case '"' -> keyBuffer.append('"');
                case '\\' -> keyBuffer.append('\\');
                case '/' -> keyBuffer.append('/');
                case 'b' -> keyBuffer.append('\b');
                case 'f' -> keyBuffer.append('\f');
                case 'n' -> keyBuffer.append('\n');
                case 'r' -> keyBuffer.append('\r');
                case 't' -> keyBuffer.append('\t');
                case 'u' -> {
                    keyUnicodeRemaining = 4;
                    keyUnicodeValue = 0;
                }
                default -> keyBuffer.append(c);
            }

            keyEscape = false;
            return;
        }

        if (c == '\\') {
            keyEscape = true;
            return;
        }

        if (c == '"') {
            lastKey = keyBuffer.toString();
            expectingColon = true;
            state = State.OUTSIDE;
            return;
        }

        keyBuffer.append(c);
    }

    private void handleSkipString(char c) {
        if (skipEscape) {
            skipEscape = false;
            return;
        }

        if (c == '\\') {
            skipEscape = true;
            return;
        }

        if (c == '"') {
            state = State.OUTSIDE;
        }
    }

    private void handleTargetString(char c, Map<String, StringBuilder> result) {
        if (targetUnicodeRemaining > 0) {
            int hex = Character.digit(c, 16);

            if (hex >= 0) {
                targetUnicodeValue = (targetUnicodeValue << 4) + hex;
                targetUnicodeRemaining--;

                if (targetUnicodeRemaining == 0) {
                    appendCharResult(result, currentValueField, (char) targetUnicodeValue);
                }
            } else {
                // 非法 unicode 转义容错。
                targetUnicodeRemaining = 0;
                targetUnicodeValue = 0;
                appendCharResult(result, currentValueField, c);
            }

            return;
        }

        if (targetEscape) {
            switch (c) {
                case '"' -> appendTextResult(result, currentValueField, "\"");
                case '\\' -> appendTextResult(result, currentValueField, "\\");
                case '/' -> appendTextResult(result, currentValueField, "/");
                case 'b' -> appendTextResult(result, currentValueField, "\b");
                case 'f' -> appendTextResult(result, currentValueField, "\f");
                case 'n' -> appendTextResult(result, currentValueField, "\n");
                case 'r' -> appendTextResult(result, currentValueField, "\r");
                case 't' -> appendTextResult(result, currentValueField, "\t");
                case 'u' -> {
                    targetUnicodeRemaining = 4;
                    targetUnicodeValue = 0;
                }
                default -> appendCharResult(result, currentValueField, c);
            }

            targetEscape = false;
            return;
        }

        if (c == '\\') {
            targetEscape = true;
            return;
        }

        if (c == '"') {
            flushPendingTargetHighSurrogate(result);

            currentValueField = null;
            targetEscape = false;
            targetUnicodeRemaining = 0;
            targetUnicodeValue = 0;

            state = State.OUTSIDE;
            return;
        }

        appendCharResult(result, currentValueField, c);
    }

    private void appendCharResult(Map<String, StringBuilder> result, String field, char c) {
        if (field == null) {
            return;
        }

        if (pendingTargetHighSurrogate != 0) {
            if (Character.isLowSurrogate(c)) {
                appendTextResult(result, field, new String(new char[]{pendingTargetHighSurrogate, c}));
                pendingTargetHighSurrogate = 0;
                return;
            }

            // 高代理后没有跟低代理，按孤立字符 flush。
            appendTextResult(result, field, String.valueOf(pendingTargetHighSurrogate));
            pendingTargetHighSurrogate = 0;
        }

        if (Character.isHighSurrogate(c)) {
            pendingTargetHighSurrogate = c;
            return;
        }

        appendTextResult(result, field, String.valueOf(c));
    }

    private void flushPendingTargetHighSurrogate(Map<String, StringBuilder> result) {
        if (pendingTargetHighSurrogate == 0) {
            return;
        }

        appendTextResult(result, currentValueField, String.valueOf(pendingTargetHighSurrogate));
        pendingTargetHighSurrogate = 0;
    }

    private void appendTextResult(Map<String, StringBuilder> result, String field, String value) {
        if (field == null || value == null || value.isEmpty()) {
            return;
        }

        result.computeIfAbsent(field, ignored -> new StringBuilder()).append(value);
    }
}