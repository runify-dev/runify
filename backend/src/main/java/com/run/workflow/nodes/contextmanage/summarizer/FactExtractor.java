package com.run.workflow.nodes.contextmanage.summarizer;

import com.run.ai.openai.chat.ChatCompletionMessageParam;
import com.run.ai.openai.chat.ChatCompletionSystemMessageParam;
import com.run.ai.openai.chat.ChatCompletionUserMessageParam;
import com.run.common.util.ChatCompletionAccumulator;
import com.run.workflow.nodes.contextmanage.service.ContextService;
import com.run.workflow.nodes.contextmanage.summarizer.impl.ToolCallSummaryGenerator;

import java.util.List;

/**
 * 便签提取器（供独立的"AI 便签提取"节点使用）：只抽便签、不产摘要。
 * <p>
 * 与摘要器共用同一套 LLM 基建（{@link SummaryGenerator#loadModel} / {@link SummaryGenerator#invokeBlocking}）
 * 与便签 schema / 解析（{@link ToolCallSummaryGenerator}），保证两处产出的便签结构、子区语义完全一致。
 * <p>
 * 与 AIChat 一致：只挂 tools、<b>不强制 tool_choice</b>，思考模式模型可用；
 * 模型未调工具而返回文本时按标签块回退解析。任何失败向上抛，由调用节点决定放行。
 */
public final class FactExtractor {

    /**
     * 便签提取的系统提示词基底（不涉及摘要，只强调抽取质量）
     */
    private static final String EXTRACT_RULES = """
            你是对话便签提取器。从给定的对话片段里抽取需要跨轮次记住的稳定事实，写入对应便签子区：
            - 只抽稳定、明确、有长期价值的信息（用户约定/称呼/偏好、任务目标与验收标准、未完成事项、环境事实等）
            - 不要抽过程细节、临时状态、寒暄、猜测或示例占位内容
            - 没有内容的子区不要提交；宁缺毋滥
            """;

    private FactExtractor() {
    }

    /**
     * 抽取诊断追踪：记录模型这次实际吐出的原文，供 debug dump 定位"为什么没抽出便签"。
     */
    public static final class Trace {
        public String finishReason;
        public int toolCalls;
        /** 模型返回的工具参数原文（诊断坏 JSON 的关键） */
        public String rawToolArguments;
        public boolean toolArgumentsValid;
        /** 模型返回的文本内容（prompt 方案主路径 / fc 回退路径用） */
        public String rawContent;
    }

    public static List<ContextService.Fact> extract(String modelId, String method, String segmentText,
                                                    List<ContextService.SectionDef> sections) throws Exception {
        return extract(modelId, method, segmentText, sections, new Trace());
    }

    /**
     * 抽取便签。
     *
     * @param modelId     模型 id
     * @param method      fc（工具调用，默认）| prompt（标签块，兼容无 FC 能力模型）
     * @param segmentText 待抽取的对话片段原文
     * @param sections    启用的便签子区（内置+自定义统一，来自便签设置；description 驱动 AI 抽取）
     * @param trace       诊断追踪（填充模型原始返回）
     * @return 抽取到的便签（可能为空）
     */
    public static List<ContextService.Fact> extract(String modelId, String method, String segmentText,
                                                    List<ContextService.SectionDef> sections,
                                                    Trace trace) throws Exception {
        if ("prompt".equals(method)) {
            return extractByPrompt(modelId, segmentText, sections, trace);
        }
        return extractByTool(modelId, segmentText, sections, trace);
    }

    /**
     * 工具调用方案：便签 schema（不含 summary）+ 不强制 tool_choice；无工具调用时回退标签块文本解析
     */
    private static List<ContextService.Fact> extractByTool(String modelId, String segmentText,
                                                           List<ContextService.SectionDef> sections, Trace trace) throws Exception {
        SummaryGenerator.LoadedModel loaded = SummaryGenerator.loadModel(modelId);
        loaded.params().put("tools", List.of(ToolCallSummaryGenerator.buildToolSchema(sections, false)));

        List<ChatCompletionMessageParam> messages = List.of(
                ChatCompletionMessageParam.ofSystem(ChatCompletionSystemMessageParam.builder()
                        .content(EXTRACT_RULES + "\n通过调用工具 " + ToolCallSummaryGenerator.TOOL_NAME
                                + " 提交便签；没有内容的字段不要传；禁止提交示例、占位或猜测的内容。").build()),
                ChatCompletionMessageParam.ofUser(ChatCompletionUserMessageParam.builder()
                        .content("对话片段：\n" + segmentText + "\n\n调用工具提交需要记住的便签：").build()));

        ChatCompletionAccumulator.AccumulatedResult result = SummaryGenerator.invokeBlocking(loaded, messages);
        trace.finishReason = result.getFinishReason();
        trace.toolCalls = result.getToolCalls().size();
        trace.rawContent = result.getContent();

        // 主路径：工具参数（parseToolArguments 遇坏 JSON 不抛，返回空）
        List<ContextService.Fact> facts = List.of();
        for (ChatCompletionAccumulator.AccumulatedToolCall call : result.getToolCalls()) {
            if (!ToolCallSummaryGenerator.TOOL_NAME.equals(call.getFunctionName())) {
                continue;
            }
            trace.rawToolArguments = call.getFunctionArguments();
            Boolean valid = result.getToolCallArgumentsValid().get(ToolCallSummaryGenerator.TOOL_NAME);
            trace.toolArgumentsValid = Boolean.TRUE.equals(valid);
            facts = ToolCallSummaryGenerator.parseToolArguments(call.getFunctionArguments(), sections).facts();
            break;
        }
        if (!facts.isEmpty()) {
            return facts;
        }

        // 回退：模型没调工具、或工具参数是坏 JSON 抽不出 → 用文本标签块再试一次
        String content = result.getContent();
        if (content != null && !content.isBlank()) {
            return SummaryGenerator.parseTaggedOutput(content, sections).facts();
        }
        return List.of();
    }

    /**
     * 提示词方案：让模型只按启用子区输出标签块，文本解析还原便签
     */
    private static List<ContextService.Fact> extractByPrompt(String modelId, String segmentText,
                                                             List<ContextService.SectionDef> sections, Trace trace) throws Exception {
        SummaryGenerator.LoadedModel loaded = SummaryGenerator.loadModel(modelId);

        StringBuilder system = new StringBuilder(EXTRACT_RULES);
        system.append("\n按下列标签块输出（无内容的块省略，不要输出其它文字）：\n");
        for (var entry : SummaryGenerator.effectiveSections(sections).entrySet()) {
            String tag = entry.getValue().tag();
            system.append("<").append(tag).append("> ").append(entry.getValue().description())
                    .append(" </").append(tag).append(">\n");
        }

        List<ChatCompletionMessageParam> messages = List.of(
                ChatCompletionMessageParam.ofSystem(ChatCompletionSystemMessageParam.builder()
                        .content(system.toString()).build()),
                ChatCompletionMessageParam.ofUser(ChatCompletionUserMessageParam.builder()
                        .content("对话片段：\n" + segmentText + "\n\n按格式输出便签块：").build()));

        ChatCompletionAccumulator.AccumulatedResult result = SummaryGenerator.invokeBlocking(loaded, messages);
        trace.finishReason = result.getFinishReason();
        trace.rawContent = result.getContent();
        String content = result.getContent();
        if (content == null || content.isBlank()) {
            return List.of();
        }
        return SummaryGenerator.parseTaggedOutput(content, sections).facts();
    }
}
