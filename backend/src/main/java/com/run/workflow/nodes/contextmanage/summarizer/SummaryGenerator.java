package com.run.workflow.nodes.contextmanage.summarizer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.run.RunApplication;
import com.run.ai.openai.chat.ChatCompletionMessageParam;
import com.run.common.util.ChatCompletionAccumulator;
import com.run.common.util.JacksonUtils;
import com.run.common.util.RSAUtil;
import com.run.dao.entity.Model;
import com.run.models.ChatModel;
import com.run.models.IProvider;
import com.run.models.ModelProvideConstants;
import com.run.workflow.nodes.contextmanage.service.ContextService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM 摘要生成器抽象（参考 run_command 的设计：接口 + 共享静态设施 + impl/ 子包放实现）。
 * <p>
 * 两个实现语义一致：输入 = 旧摘要 + 待摘段原文 + 启用的便签子区，输出 = 合并摘要 + 还流便签；
 * 只在"规则压完仍超低水位"时被 ContextService 调用一次（阈值触发，非每轮）；
 * 任何失败向上抛，由 ContextService 回退抽取式，不阻断。
 * <ul>
 *     <li>{@code impl/ToolCallSummaryGenerator}（默认）：单工具 + tool_choice 强制，schema 保证结构；
 *     模型不调工具时按标签块文本回退解析</li>
 *     <li>{@code impl/PromptSummaryGenerator}：提示词 + 标签块解析，兼容无 FC 能力的模型</li>
 * </ul>
 */
public interface SummaryGenerator extends ContextService.Summarizer {

    long LLM_TIMEOUT_SECONDS = 60;

    /**
     * 内置子区 → [固定英文标签, 默认提示词说明]。标签是内部管道（解析锚点 + prompt cache 友好），恒定；
     * 说明只是<b>兜底</b>——便签设置里的 description 优先，用户清空时才回退这里（见 effectiveSections）。
     */
    Map<String, String[]> SECTIONS = Collections.unmodifiableMap(new LinkedHashMap<>() {{
        put("convention", new String[]{"CONVENTIONS", "项目约定：规则/规范/禁忌（面向任务本身，非个人）"});
        put("preference", new String[]{"PREFERENCES", "用户个人喜好：称呼/语言/语气/风格倾向/口味（跟人走）"});
        put("env", new String[]{"ENV", "环境事实：依赖/版本/路径/配置"});
        put("goal", new String[]{"GOALS", "任务目标/验收标准/约束"});
        put("todo", new String[]{"TODO", "未完成事项/下一步"});
    }});

    /**
     * 摘要质量要求（两个实现共用的提示词基底）
     */
    String SUMMARY_RULES = """
            你是对话上下文压缩器。把给定的对话片段压缩成简洁的要点摘要，要求：
            - 保留：任务目标与约束、关键决策与结论、重要文件/路径/版本号、报错与修复结果、未完成事项
            - 丢弃：寒暄、过程性细节、重复内容、工具调用的原始输出
            - 若提供了已有摘要，在其基础上增量合并，不得丢失其中的信息
            - 摘要为纯文本要点列表，每行一条，以 - 开头
            """;

    // ══════════════════════════ 接口方法 ══════════════════════════

    /**
     * 生成合并摘要与还流便签（阻塞调用，超时 {@link #LLM_TIMEOUT_SECONDS} 秒）
     */
    @Override
    ContextService.SummarizedNotes summarize(String previousDigest, String segmentText,
                                             List<ContextService.SectionDef> sections) throws Exception;

    // ══════════════════════════ 共享基础设施 ══════════════════════════

    /**
     * 已装配模型：llm 客户端 + 请求参数（凭证解密后的连接参数 + 模型参数表默认值，
     * 实现可继续往 params 里塞 tools / tool_choice，整个 map 会透传进请求体）
     */
    record LoadedModel(ChatModel llm, Map<String, Object> params) {
    }

    /**
     * 按模型 id 装配 ChatModel（与 AIChat 相同路径：取模型 → RSA 解密凭证 → 参数表默认值 → provider）
     */
    static LoadedModel loadModel(String modelId) throws Exception {
        Model model = RunApplication.appComponent.modelMapper().getById(modelId)
                .toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        if (model == null) {
            throw new RuntimeException("摘要模型不存在: " + modelId);
        }
        IProvider provider = ModelProvideConstants.valueOf(model.getProvider()).getProvider();
        String decrypt = RSAUtil.decrypt(model.getCredential());
        Map<String, Object> params = JacksonUtils.fromJson(decrypt, new TypeReference<Map<String, Object>>() {
        });
        JsonArray modelParameterForm = model.getModelParameterForm();
        for (int i = 0; i < modelParameterForm.size(); i++) {
            JsonObject jsonObject = modelParameterForm.getJsonObject(i);
            params.put(jsonObject.getString("field"), jsonObject.getValue("defaultValue"));
        }
        params.put("stream", false);
        ChatModel llm = provider.getModel(model.getModelType(), model.getModelName(), params, Map.of(), ChatModel.class);
        return new LoadedModel(llm, params);
    }

    /**
     * 非流式调用并聚合结果（阻塞，带超时）
     */
    static ChatCompletionAccumulator.AccumulatedResult invokeBlocking(
            LoadedModel loaded, List<ChatCompletionMessageParam> messages) throws Exception {
        var completion = loaded.llm().invoke(messages, new JsonObject(loaded.params()))
                .get(LLM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        ChatCompletionAccumulator accumulator =
                new ChatCompletionAccumulator(List.of("reasoning_content", "reasoning"), List.of());
        accumulator.append(completion);
        return accumulator.complete();
    }

    static String buildUserPrompt(String previousDigest, String segmentText, String instruction) {
        return "已有摘要：\n"
                + (StringUtils.isBlank(previousDigest) ? "（无）" : previousDigest)
                + "\n\n新增对话片段：\n" + segmentText
                + "\n\n" + instruction;
    }

    // ══════════════════════════ 标签块文本解析（提示词实现主路径 / FC 实现回退路径） ══════════════════════════

    /**
     * 解析后的子区：tag = 提示词/解析用标签（内置固定英文、自定义 = key），
     * description = 驱动抽取的说明（<b>库值优先</b>，空回退内置默认文案），listStyle 来自便签设置。
     */
    record ResolvedSection(String tag, String description, boolean listStyle) {
    }

    /**
     * 有效子区（key → 解析结果），迭代顺序 = 便签设置 sortOrder。同 key 首见生效。
     * 供 schema / 提示词 / 解析统一迭代——抽取侧与渲染侧同源，均由库配置驱动。
     */
    static Map<String, ResolvedSection> effectiveSections(List<ContextService.SectionDef> sections) {
        Map<String, ResolvedSection> eff = new LinkedHashMap<>();
        if (sections == null) {
            return eff;
        }
        for (ContextService.SectionDef s : sections) {
            if (s == null || StringUtils.isBlank(s.key())) {
                continue;
            }
            String[] builtin = SECTIONS.get(s.key());
            String tag = builtin != null ? builtin[0] : s.key();
            String desc = StringUtils.isNotBlank(s.description()) ? s.description()
                    : (builtin != null ? builtin[1] : "");
            eff.putIfAbsent(s.key(), new ResolvedSection(tag, desc, s.listStyle()));
        }
        return eff;
    }

    /**
     * 解析"摘要正文 + 标签块"格式的模型输出：块摘出成便签，剩余部分为摘要。纯函数，可单测。
     */
    static ContextService.SummarizedNotes parseTaggedOutput(String content, List<ContextService.SectionDef> sections) {
        List<ContextService.Fact> facts = new ArrayList<>();
        String remaining = content;
        for (Map.Entry<String, ResolvedSection> entry : effectiveSections(sections).entrySet()) {
            String tag = entry.getValue().tag();
            Pattern pattern = Pattern.compile("<" + Pattern.quote(tag) + ">([\\s\\S]*?)</" + Pattern.quote(tag) + ">");
            Matcher matcher = pattern.matcher(remaining);
            while (matcher.find()) {
                parseFactLines(matcher.group(1), entry.getKey(), facts);
            }
            remaining = matcher.reset().replaceAll("");
        }
        return new ContextService.SummarizedNotes(remaining.strip(), facts);
    }

    /**
     * 块内行解析：`- 键: 值`；无分隔符的列表行（目标/待办）整行作值、键取前缀做幂等去重
     */
    private static void parseFactLines(String block, String subtype, List<ContextService.Fact> facts) {
        for (String rawLine : block.split("\n")) {
            String line = rawLine.strip();
            if (line.startsWith("-") || line.startsWith("*")) {
                line = line.substring(1).strip();
            }
            if (StringUtils.isBlank(line)) {
                continue;
            }
            int sep = indexOfSeparator(line);
            String key;
            String value;
            if (sep > 0) {
                key = line.substring(0, sep).strip();
                value = line.substring(sep + 1).strip();
            } else {
                key = StringUtils.abbreviate(line, 64);
                value = line;
            }
            if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                facts.add(new ContextService.Fact(subtype, key, value));
            }
        }
    }

    private static int indexOfSeparator(String line) {
        int colon = line.indexOf(':');
        int fullColon = line.indexOf('：');
        if (colon < 0) {
            return fullColon;
        }
        if (fullColon < 0) {
            return colon;
        }
        return Math.min(colon, fullColon);
    }
}
