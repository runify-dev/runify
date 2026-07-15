package com.run.workflow.nodes.contextmanage.summarizer.impl;

import com.run.ai.openai.chat.ChatCompletionMessageParam;
import com.run.ai.openai.chat.ChatCompletionSystemMessageParam;
import com.run.ai.openai.chat.ChatCompletionUserMessageParam;
import com.run.common.util.ChatCompletionAccumulator;
import com.run.workflow.nodes.contextmanage.service.ContextService;
import com.run.workflow.nodes.contextmanage.summarizer.SummaryGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 提示词方案：让模型在摘要正文之后按启用子区附
 * {@code <CONVENTIONS> <ENV> <GOALS> <TODO>} 标签块，文本解析还流。
 * <p>
 * 适合 function calling 能力弱/缺失的摘要模型；解析失败只丢块不丢摘要。
 */
@Slf4j
public final class PromptSummaryGenerator implements SummaryGenerator {

    private final String modelId;

    public PromptSummaryGenerator(String modelId) {
        this.modelId = modelId;
    }

    @Override
    public ContextService.SummarizedNotes summarize(String previousDigest, String segmentText,
                                                    List<ContextService.SectionDef> sections) throws Exception {
        LoadedModel loaded = SummaryGenerator.loadModel(modelId);

        List<ChatCompletionMessageParam> messages = List.of(
                ChatCompletionMessageParam.ofSystem(ChatCompletionSystemMessageParam.builder()
                        .content(buildSystemPrompt(sections)).build()),
                ChatCompletionMessageParam.ofUser(ChatCompletionUserMessageParam.builder()
                        .content(SummaryGenerator.buildUserPrompt(previousDigest, segmentText,
                                "按格式输出合并后的摘要与沉淀块：")).build()));

        ChatCompletionAccumulator.AccumulatedResult result = SummaryGenerator.invokeBlocking(loaded, messages);
        String content = result.getContent();
        if (StringUtils.isBlank(content)) {
            throw new RuntimeException("摘要模型返回空内容");
        }

        ContextService.SummarizedNotes notes = SummaryGenerator.parseTaggedOutput(content, sections);
        if (StringUtils.isBlank(notes.summary())) {
            throw new RuntimeException("摘要模型输出中没有摘要正文");
        }
        log.debug("context-manage 提示词摘要完成：摘要 {} 字，还流便签 {} 条",
                notes.summary().length(), notes.facts().size());
        return notes;
    }

    private String buildSystemPrompt(List<ContextService.SectionDef> sections) {
        StringBuilder sb = new StringBuilder(SUMMARY_RULES);
        sb.append("""

                输出格式：
                1. 先输出合并后的摘要要点列表，不要任何标题或额外说明。
                """);
        StringBuilder blocks = new StringBuilder();
        // 子区、标签、说明全部来自便签设置（effectiveSections：库 description 优先、空回退内置默认）
        for (Map.Entry<String, SummaryGenerator.ResolvedSection> entry
                : SummaryGenerator.effectiveSections(sections).entrySet()) {
            SummaryGenerator.ResolvedSection meta = entry.getValue();
            blocks.append("<").append(meta.tag()).append(">\n")
                    .append(meta.listStyle() ? "- 值" : "- 键: 值").append("\n</").append(meta.tag())
                    .append(">  （").append(meta.description()).append("）\n");
        }
        if (!blocks.isEmpty()) {
            sb.append("""
                    2. 摘要之后，如果片段中出现了值得跨轮次记住的事实，按下列标签块输出（块内每行格式 `- 键: 值`；
                       某类没有内容就完全不要输出该块；禁止输出示例、占位或猜测的内容）：
                    """).append(blocks);
        }
        return sb.toString();
    }
}
