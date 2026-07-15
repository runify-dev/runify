package com.run.workflow.nodes.contextmanage.summarizer.impl;

import com.run.ai.openai.chat.ChatCompletionMessageParam;
import com.run.ai.openai.chat.ChatCompletionSystemMessageParam;
import com.run.ai.openai.chat.ChatCompletionUserMessageParam;
import com.run.common.util.ChatCompletionAccumulator;
import com.run.workflow.nodes.contextmanage.service.ContextService;
import com.run.workflow.nodes.contextmanage.summarizer.SummaryGenerator;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具调用方案（默认）：单工具 {@code submit_context_digest} + {@code tool_choice} 强制，
 * JSON schema 保证摘要与各便签子区的结构，无需文本解析。
 * <p>
 * 降级链：模型没调工具但给了文本 → 按标签块回退解析；参数 JSON 不可解析 / 空摘要 → 抛出，
 * 由 ContextService 回退抽取式。
 */
@Slf4j
public final class ToolCallSummaryGenerator implements SummaryGenerator {

    public static final String TOOL_NAME = "submit_context_digest";

    /**
     * 内置子区 → 工具参数字段名（内部管道，恒定；自定义子区字段名 = key）
     */
    private static final Map<String, String> SECTION_FIELDS = Map.of(
            "convention", "conventions",
            "preference", "preferences",
            "env", "env",
            "goal", "goals",
            "todo", "todos");

    private final String modelId;

    public ToolCallSummaryGenerator(String modelId) {
        this.modelId = modelId;
    }

    @Override
    public ContextService.SummarizedNotes summarize(String previousDigest, String segmentText,
                                                    List<ContextService.SectionDef> sections) throws Exception {
        LoadedModel loaded = SummaryGenerator.loadModel(modelId);
        // 与 AIChat 一致：只挂 tools，不强制 tool_choice。
        // 思考模式模型（如 deepseek reasoner）不支持 tool_choice 强制指定函数，会直接 HTTP 400；
        // 交给模型 auto 决定——能力强的模型仍会主动调 submit_context_digest 走结构化主路径，
        // 未调工具而返回文本的模型由下方标签块回退解析兜底。
        loaded.params().put("tools", List.of(buildToolSchema(sections, true)));

        List<ChatCompletionMessageParam> messages = List.of(
                ChatCompletionMessageParam.ofSystem(ChatCompletionSystemMessageParam.builder()
                        .content(SUMMARY_RULES + "\n通过调用工具 " + TOOL_NAME
                                + " 提交结果；没有内容的便签字段不要传；禁止提交示例、占位或猜测的内容。").build()),
                ChatCompletionMessageParam.ofUser(ChatCompletionUserMessageParam.builder()
                        .content(SummaryGenerator.buildUserPrompt(previousDigest, segmentText,
                                "调用工具提交合并后的摘要与沉淀便签：")).build()));

        ChatCompletionAccumulator.AccumulatedResult result = SummaryGenerator.invokeBlocking(loaded, messages);

        // 主路径：工具参数
        for (ChatCompletionAccumulator.AccumulatedToolCall call : result.getToolCalls()) {
            if (!TOOL_NAME.equals(call.getFunctionName())) {
                continue;
            }
            ContextService.SummarizedNotes notes = parseToolArguments(call.getFunctionArguments(), sections);
            if (StringUtils.isNotBlank(notes.summary())) {
                log.debug("context-manage 工具摘要完成：摘要 {} 字，还流便签 {} 条",
                        notes.summary().length(), notes.facts().size());
                return notes;
            }
        }

        // 回退路径：模型没调工具（或参数无摘要）但给了文本 → 按标签块解析
        String content = result.getContent();
        if (StringUtils.isNotBlank(content)) {
            ContextService.SummarizedNotes notes = SummaryGenerator.parseTaggedOutput(content, sections);
            if (StringUtils.isNotBlank(notes.summary())) {
                log.debug("context-manage 工具摘要回退文本解析：摘要 {} 字，还流便签 {} 条",
                        notes.summary().length(), notes.facts().size());
                return notes;
            }
        }
        throw new RuntimeException("摘要模型未提交有效结果（无工具调用且无可解析文本）");
    }

    /**
     * 按启用子区（库配置驱动，含内置 + 自定义）构建工具 schema。
     * 列表型（便签设置 list_style=true）为字符串数组，其余为 {key,value} 对象数组；
     * 字段描述 = 便签设置的抽取说明（空回退内置默认）。
     *
     * @param includeSummary true 含 summary 字段且必填（摘要器用）；
     *                       false 仅便签字段、无必填（便签提取节点复用，不产摘要）
     */
    public static Map<String, Object> buildToolSchema(List<ContextService.SectionDef> sections,
                                                      boolean includeSummary) {
        Map<String, Object> properties = new HashMap<>();
        if (includeSummary) {
            properties.put("summary", Map.of(
                    "type", "string",
                    "description", "合并后的摘要要点列表，每行一条，以 - 开头"));
        }
        for (Map.Entry<String, SummaryGenerator.ResolvedSection> entry
                : SummaryGenerator.effectiveSections(sections).entrySet()) {
            String section = entry.getKey();
            String desc = entry.getValue().description();
            String fieldName = SECTION_FIELDS.getOrDefault(section, section);
            if (entry.getValue().listStyle()) {
                properties.put(fieldName, Map.of(
                        "type", "array",
                        "description", desc,
                        "items", Map.of("type", "string")));
            } else {
                properties.put(fieldName, Map.of(
                        "type", "array",
                        "description", desc,
                        "items", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "key", Map.of("type", "string"),
                                        "value", Map.of("type", "string")),
                                "required", List.of("key", "value"))));
            }
        }
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", TOOL_NAME,
                        "description", "提交压缩后的对话摘要与需要跨轮次记住的便签",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", properties,
                                "required", includeSummary ? List.of("summary") : List.of())));
    }

    /**
     * 解析工具参数 JSON → 摘要 + 便签。纯函数，可单测。
     * <p>
     * JSON 不可解析时<b>不抛出</b>（LLM 常返回缺逗号/未转义引号的坏 JSON，中文值尤甚），
     * 返回空结果，由调用方回退到文本标签块解析——坏 JSON 不该炸掉整个提取。
     */
    public static ContextService.SummarizedNotes parseToolArguments(String argumentsJson,
                                                                    List<ContextService.SectionDef> sections) {
        JsonObject args;
        try {
            args = new JsonObject(argumentsJson);
        } catch (Exception e) {
            log.warn("context-manage 工具参数非法 JSON，回退文本解析（前 200 字）: {}",
                    StringUtils.abbreviate(argumentsJson, 200));
            return new ContextService.SummarizedNotes("", new ArrayList<>());
        }
        String summary = StringUtils.defaultString(args.getString("summary")).strip();
        List<ContextService.Fact> facts = new ArrayList<>();
        for (Map.Entry<String, SummaryGenerator.ResolvedSection> entry
                : SummaryGenerator.effectiveSections(sections).entrySet()) {
            String section = entry.getKey();
            JsonArray array = args.getJsonArray(SECTION_FIELDS.getOrDefault(section, section), new JsonArray());
            for (Object element : array) {
                if (entry.getValue().listStyle() && element instanceof String s) {
                    String value = s.strip();
                    if (StringUtils.isNotBlank(value)) {
                        facts.add(new ContextService.Fact(section, StringUtils.abbreviate(value, 64), value));
                    }
                } else if (element instanceof JsonObject jo) {
                    String key = StringUtils.defaultString(jo.getString("key")).strip();
                    String value = StringUtils.defaultString(jo.getString("value")).strip();
                    if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                        facts.add(new ContextService.Fact(section, key, value));
                    }
                } else if (element instanceof Map<?, ?> map) {
                    Object key = map.get("key");
                    Object value = map.get("value");
                    if (key != null && value != null
                            && StringUtils.isNotBlank(key.toString()) && StringUtils.isNotBlank(value.toString())) {
                        facts.add(new ContextService.Fact(section, key.toString().strip(), value.toString().strip()));
                    }
                }
            }
        }
        return new ContextService.SummarizedNotes(summary, facts);
    }
}
