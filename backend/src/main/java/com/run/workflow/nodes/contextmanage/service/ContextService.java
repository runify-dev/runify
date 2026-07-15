package com.run.workflow.nodes.contextmanage.service;

import com.run.common.constants.ContentTypeConstants;
import com.run.workflow.nodes.contextmanage.service.ContentClassifier.ToolClass;
import com.run.workflow.nodes.contextmanage.service.ContentClassifier.ToolKind;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 上下文压缩纯逻辑（不做任何 I/O，可单测）。
 * <p>
 * 运行时模型：只读循环变量（= 载入的历史 + 运行中 push 的增量），内存打标记压缩 → 渲染投影；
 * <b>不查表、不用库游标</b>。
 * <p>
 * 摘要状态显式进出：{@code (summaryText, summaryCovered)} 由调用方传入（通常来自纳管的摘要变量，
 * 跨迭代持久），本轮可能推进后随 Result 传出、由调用方写回。covered 是<b>过滤后时间线的前缀条数</b>
 * ——循环变量只增不减，前缀游标天然稳定；已覆盖条目不再参与计量与摘要，
 * 增量滚动真正成立（稳态下 LLM 摘要不会每轮重调）。前缀内的 SYSTEM 与首条真实用户任务保持原文输出。
 * <p>
 * 层级过滤（errata 纠正 6）：带循环层级 position（index != null）的 TOOL 条目
 * 是载入历史或子 agent 的内部往返，投影时直接丢弃；QUESTION/TEXT 会话表层始终保留。
 * <p>
 * 每轮基于原文重新打标（幂等），标记只在内存，不落库、不改原文。
 */
public class ContextService {

    /**
     * 处置标记：信息损失从小到大
     */
    public enum Disposition {
        LIVE, ARG_STUB, STUBBED, TRUNCATED, FOLDED, SUMMARIZED
    }

    public record Config(
            int budget,
            double highRatio,
            double lowRatio,
            int keepRecentItems,
            int stubAge,
            int truncAge,
            boolean stripMultimodal,
            int reservedTokens,
            String tokenEncoding,
            /**
             * 启用的便签子区定义（内置+自定义统一，按便签设置的 sortOrder 排序，库配置驱动）：
             * 抽取（schema/提示词/解析）与渲染（标题/顺序/列表型限量）共用这一份，不再查硬编码表。
             */
            List<SectionDef> sections) {

        public static Config defaults() {
            List<SectionDef> sections = SectionRegistry.BUILTIN_DEFAULTS.stream()
                    .map(s -> new SectionDef(s.key(), s.label(), s.description(), s.listStyle()))
                    .toList();
            return new Config(32000, 0.85, 0.6, 10, 30, 15, true, 3000, "cl100k", sections);
        }
    }

    /**
     * 便签：产物也是便签（subtype = "artifact"，key = 文件路径，value = 状态），
     * 三类数据（上下文 / 摘要 / 便签）之外不再有第四类
     */
    public record Fact(String subtype, String key, String value) {
    }

    /**
     * 便签子区定义：key = 子区标识（= 便签 subtype，存库），label = 显示标题，
     * description = 告诉模型"抽什么"的说明，listStyle = 列表型（渲染时去重并限量）。
     */
    public record SectionDef(String key, String label, String description, boolean listStyle) {
        public SectionDef(String key, String label, String description) {
            this(key, label, description, false);
        }
    }

    /**
     * 产物子区标识
     */
    public static final String ARTIFACT_SUBTYPE = "artifact";

    /**
     * LLM 摘要产出：摘要正文 + 多通道还流的便签（按启用子区解析）
     */
    public record SummarizedNotes(String summary, List<Fact> facts) {
    }

    /**
     * LLM 摘要器（可选）：只在"规则压完仍超低水位"时被调用一次；
     * 抛异常 / 返回空摘要 → 回退抽取式，不阻断。
     */
    @FunctionalInterface
    public interface Summarizer {
        SummarizedNotes summarize(String previousDigest, String segmentText, List<SectionDef> sections) throws Exception;
    }

    /**
     * @param seedContents   上下文初始值（载入的历史，每次运行固定）
     * @param contents       上下文变化值（运行中 push 的增量）
     * @param facts          当前便签（含 artifact 子区）
     * @param summaryText    当前摘要正文（可空）
     * @param summaryCovered 摘要已覆盖的（过滤后拼接时间线）前缀条数
     */
    public record Input(
            List<JsonObject> seedContents,
            List<JsonObject> contents,
            List<Fact> facts,
            String summaryText,
            int summaryCovered) {
    }

    /**
     * @param messages       完整投影（便签 system 旁注 + 摘要消息 + 按标记渲染的条目），喂 ai-chat
     * @param summaryText    新摘要正文（调用方写回摘要变化值）
     * @param summaryCovered 新覆盖前缀条数
     * @param seedCovered    摘要是否已完整覆盖上下文初始值（写入节点据此推进跨对话游标）
     * @param factsToWrite   本轮蒸馏/还流的便签增量（含 artifact 子区）
     * @param coveredUpto    覆盖前缀对应的历史时间戳（写入节点据此推进跨对话游标，行边界安全）；
     *                       无已覆盖的历史条目时为 null。部分覆盖也会给出（= 最后一个"整行都已覆盖"的历史条目时间）
     */
    public record Result(
            List<Map<String, Object>> messages,
            String summaryText,
            int summaryCovered,
            boolean seedCovered,
            List<Fact> factsToWrite,
            Map<String, Object> stats,
            String coveredUpto) {
    }

    /**
     * 时间线条目（内存标记的载体，不动原文、不落库）
     */
    private static final class Item {
        JsonObject content;
        ContentTypeConstants type;
        int index;
        boolean digest;
        ToolClass toolClass;
        Disposition disposition = Disposition.LIVE;
        String stubText;
        String stubArguments;
        int tokens;
        /**
         * 来源历史条目的创建时间（仅初始值/载入历史带；变化值为运行内新增，无此值）。
         * 用于跨对话游标推进；不参与渲染（组装时已从渲染内容中剥离）
         */
        String sourceTime;
    }

    public static final String DIGEST_PREFIX = "[历史对话摘要]";
    private static final String DIGEST_HEADER = DIGEST_PREFIX + " 更早内容已压缩，原文完整保存在对话记录中\n";
    private static final int LONG_CONTENT_CHARS = 2000;
    private static final int BIG_ARGUMENT_CHARS = 800;
    private static final int DIGEST_MAX_CHARS = 8000;
    /**
     * LLM 摘要的段落原文输入上限（字符）：cl100k 下约 1 万+ token，防止摘要调用本身爆预算
     */
    private static final int SEGMENT_INPUT_MAX_CHARS = 24000;
    private static final Set<ContentTypeConstants> RENDERABLE = Set.of(
            ContentTypeConstants.SYSTEM, ContentTypeConstants.QUESTION,
            ContentTypeConstants.TEXT, ContentTypeConstants.TOOL);

    private final TokenCounter tokenCounter;
    private final Summarizer summarizer;

    public ContextService(TokenCounter tokenCounter) {
        this(tokenCounter, null);
    }

    public ContextService(TokenCounter tokenCounter, Summarizer summarizer) {
        this.tokenCounter = tokenCounter;
        this.summarizer = summarizer;
    }

    public Result process(Input in, Config cfg) {
        // ── ① 组装时间线：初始值（载入历史）+ 变化值（运行增量）拼接（层级过滤 / 多模态剥离 / digest 识别）──
        List<Item> items = buildTimeline(in.seedContents(), cfg);
        int seedCount = items.size();
        for (JsonObject content : in.contents()) {
            Item item = toItem(content, cfg);
            if (item != null) {
                item.index = items.size();
                items.add(item);
            }
        }
        int firstQuestionIdx = firstRealQuestionIndex(items);

        String summary = StringUtils.defaultString(in.summaryText()).strip();
        int covered = Math.max(0, Math.min(in.summaryCovered(), items.size()));

        // ── ② 吸收 covered 起的连续 digest 条目（载入器合成的历史摘要行等），免费滚动 ──
        while (covered < items.size() && items.get(covered).digest) {
            summary = appendSummary(summary, stripDigestHeader(items.get(covered).content.getString("content", "")));
            items.get(covered).disposition = Disposition.SUMMARIZED;
            covered++;
        }

        // ── ③ 前缀标记：已覆盖条目不再输出，SYSTEM / 首条真实任务保持原文 ──
        for (int i = 0; i < covered; i++) {
            Item item = items.get(i);
            if (item.type == ContentTypeConstants.SYSTEM || i == firstQuestionIdx) {
                continue;
            }
            item.disposition = Disposition.SUMMARIZED;
        }

        // ── ④ token 计量：分母含固定开销、便签旁注与摘要 ──
        String notesText = renderNotes(in.facts(), cfg.sections());
        int notesTokens = tokenCounter.count(notesText);
        for (Item item : items) {
            item.tokens = countItem(item.content, item.type);
        }
        int base = cfg.reservedTokens() + notesTokens;
        int rawTotal = base + tokenCounter.count(summary)
                + items.stream().mapToInt(this::effectiveTokens).sum();

        int high = (int) (cfg.budget() * cfg.highRatio());
        int low = (int) (cfg.budget() * cfg.lowRatio());

        List<Fact> factsToWrite = new ArrayList<>();
        boolean llmSummarized = false;
        String summarizerFallback = null;

        // ── ⑤ 超高水位 → 规则瀑布打标，压到低水位即停 ──
        if (rawTotal > high) {
            Set<Integer> protectedIdx = protectedIndexes(items, firstQuestionIdx, cfg);

            foldProbes(items, protectedIdx, factsToWrite);
            if (effective(items, base) + tokenCounter.count(summary) > low) {
                stubInvalidatedReads(items, protectedIdx);
            }
            if (effective(items, base) + tokenCounter.count(summary) > low) {
                stubSupersededArguments(items, protectedIdx);
            }
            if (effective(items, base) + tokenCounter.count(summary) > low) {
                stubDuplicates(items, protectedIdx);
            }
            if (effective(items, base) + tokenCounter.count(summary) > low) {
                degradeByAge(items, protectedIdx, cfg);
            }

            // ── ⑥ 仍超低水位 → 摘要游标向前推进（增量滚动，只摘新增段；LLM 仅此时调一次）──
            if (effective(items, base) + tokenCounter.count(summary) > low) {
                DigestResult dr = digestForward(items, cfg, base, low, summary, covered, firstQuestionIdx);
                summary = dr.summary;
                covered = dr.covered;
                llmSummarized = dr.llm;
                summarizerFallback = dr.error;
                factsToWrite.addAll(dr.facts);
            }
        }

        // ── 产物自动登记（便签 artifact 子区）：本轮可见的成功里程碑文件写/删 ──
        // 目标/约定/待办等语义便签交给 AI 提取（fact-extract 节点 / 摘要器），
        // 不再用"抄最后一句用户话"的规则锚点——它抓不到真实目标，只会注入占位/琐碎噪音。
        factsToWrite.addAll(collectArtifactFacts(items));

        // ── ⑦ 渲染投影 ──
        List<Map<String, Object>> messages = new ArrayList<>();
        if (StringUtils.isNotBlank(notesText)) {
            messages.add(new JsonObject()
                    .put("type", ContentTypeConstants.SYSTEM.name())
                    .put("content", "# 工作便签（跨轮次沉淀，供参考）\n" + notesText)
                    .getMap());
        }
        String digestText = StringUtils.isBlank(summary) ? null : DIGEST_HEADER + summary;
        messages.addAll(renderCompressed(items, digestText, digestInsertIndex(items)));
        // ── ⑧ 严格 user/assistant 交替收口：压缩折叠掉助手回合后，保留的用户问题会相邻，
        // 严格交替的 chat 模型会报错；合并相邻同角色消息（含首部 SYSTEM）保证不出现连续同角色。
        messages = enforceAlternation(messages);

        int effectiveTotal = base + tokenCounter.count(summary)
                + items.stream().mapToInt(this::effectiveTokens).sum();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("rawTokens", rawTotal);
        stats.put("effectiveTokens", effectiveTotal);
        stats.put("items", items.size());
        stats.put("covered", covered);
        stats.put("seedItems", seedCount);
        for (Disposition d : Disposition.values()) {
            long c = items.stream().filter(i -> i.disposition == d).count();
            if (c > 0) {
                stats.put(d.name().toLowerCase(Locale.ROOT), c);
            }
        }
        if (llmSummarized) {
            stats.put("llmSummarized", true);
        }
        if (summarizerFallback != null) {
            stats.put("summarizerFallback", summarizerFallback);
        }

        String coveredUpto = computeCoveredUpto(items, covered);
        if (coveredUpto != null) {
            stats.put("coveredUpto", coveredUpto);
        }

        return new Result(messages, summary, covered, covered >= seedCount, factsToWrite, stats, coveredUpto);
    }

    /**
     * 覆盖前缀对应的历史时间戳，行边界安全：
     * 一条 conversation_message 会拍平成多个同时间戳的 Content，覆盖前缀若切在某行中间，
     * 绝不能把该行时间当作游标（否则下轮 create_time &gt; 游标 会漏掉该行未覆盖的部分）。
     * 故取"最后一个整行都已覆盖"的历史时间：= 严格早于首个未覆盖历史条目时间 的最大已覆盖历史时间。
     * 全覆盖时即最大已覆盖时间（historyUpto），无历史条目时为 null。
     */
    private static String computeCoveredUpto(List<Item> items, int covered) {
        LocalDateTime firstUncovered = null;
        for (int i = covered; i < items.size(); i++) {
            LocalDateTime t = parseTime(items.get(i).sourceTime);
            if (t != null && (firstUncovered == null || t.isBefore(firstUncovered))) {
                firstUncovered = t;
            }
        }
        LocalDateTime best = null;
        for (int i = 0; i < covered; i++) {
            LocalDateTime t = parseTime(items.get(i).sourceTime);
            if (t == null) {
                continue;
            }
            // 与未覆盖条目同时间戳（同一行跨边界）→ 跳过，只认严格更早的整行
            if (firstUncovered != null && !t.isBefore(firstUncovered)) {
                continue;
            }
            if (best == null || t.isAfter(best)) {
                best = t;
            }
        }
        return best == null ? null : best.toString();
    }

    private static LocalDateTime parseTime(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    // ══════════════════════════ 时间线组装 ══════════════════════════

    private List<Item> buildTimeline(List<JsonObject> contents, Config cfg) {
        List<Item> items = new ArrayList<>();
        for (JsonObject content : contents) {
            Item item = toItem(content, cfg);
            if (item != null) {
                item.index = items.size();
                items.add(item);
            }
        }
        return items;
    }

    private Item toItem(JsonObject content, Config cfg) {
        ContentTypeConstants type = parseType(content);
        if (type == null || !RENDERABLE.contains(type)) {
            return null;
        }
        // 层级过滤：带循环层级 position 的 TOOL = 载入历史 / 子 agent 内部往返，丢弃
        if (type == ContentTypeConstants.TOOL && hasNestedPosition(content)) {
            return null;
        }
        // 剥离载入器打的时间戳（仅用于游标推进，不能进入喂给模型的渲染内容）
        String sourceTime = content.getString("createTime");
        if (sourceTime != null) {
            content = content.copy();
            content.remove("createTime");
        }
        Item item = new Item();
        item.type = type;
        item.content = content;
        item.sourceTime = sourceTime;
        if (type == ContentTypeConstants.QUESTION) {
            item.digest = content.getString("content", "").startsWith(DIGEST_PREFIX);
            if (!item.digest && cfg.stripMultimodal() && hasFiles(content)) {
                JsonObject copy = content.copy();
                copy.remove("images");
                copy.remove("videos");
                copy.remove("files");
                item.content = copy;
            }
        }
        if (type == ContentTypeConstants.TOOL) {
            item.toolClass = ContentClassifier.classify(item.content);
        }
        return item;
    }

    private static int firstRealQuestionIndex(List<Item> items) {
        for (Item item : items) {
            if (item.type == ContentTypeConstants.QUESTION && !item.digest) {
                return item.index;
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private static boolean hasNestedPosition(JsonObject content) {
        Object position = content.getValue("position");
        JsonObject json;
        if (position instanceof JsonObject jo) {
            json = jo;
        } else if (position instanceof Map<?, ?> map) {
            json = new JsonObject((Map<String, Object>) map);
        } else {
            return false;
        }
        return json.getValue("index") != null || json.getValue("children") != null;
    }

    // ══════════════════════════ 保护区 ══════════════════════════

    /**
     * 规则瀑布不打标：SYSTEM、首条真实用户任务、最近 keepRecentItems 条（digest 条目除外）
     */
    private Set<Integer> protectedIndexes(List<Item> items, int firstQuestionIdx, Config cfg) {
        Set<Integer> result = new HashSet<>();
        for (Item item : items) {
            if (item.type == ContentTypeConstants.SYSTEM) {
                result.add(item.index);
            }
        }
        if (firstQuestionIdx >= 0) {
            result.add(firstQuestionIdx);
        }
        int from = Math.max(0, items.size() - cfg.keepRecentItems());
        for (int i = from; i < items.size(); i++) {
            if (!items.get(i).digest) {
                result.add(i);
            }
        }
        return result;
    }

    // ══════════════════════════ 规则瀑布 ══════════════════════════

    /**
     * 序 0：折叠成功探测（结论蒸馏进 env 便签）。末条不折。
     */
    private void foldProbes(List<Item> items, Set<Integer> protectedIdx, List<Fact> factsToWrite) {
        Map<String, Fact> distilled = new LinkedHashMap<>();
        for (Item item : items) {
            if (item.type != ContentTypeConstants.TOOL || item.disposition != Disposition.LIVE
                    || protectedIdx.contains(item.index)) {
                continue;
            }
            ToolClass tc = item.toolClass;
            if (tc == null || tc.kind() != ToolKind.PROBE || !Boolean.TRUE.equals(tc.ok())) {
                continue;
            }
            if (item.index == items.size() - 1) {
                continue;
            }
            item.disposition = Disposition.FOLDED;
            String key = StringUtils.defaultIfBlank(tc.resourceKey(),
                    item.content.getString("toolName", "probe"));
            String value = firstLine(item.content.getString("content", ""), 160);
            if (StringUtils.isNotBlank(value)) {
                distilled.put(key, new Fact("env", key, value));
            }
        }
        factsToWrite.addAll(distilled.values());
    }

    /**
     * 序 1a：失效键——读了 X，后文写 / 再读 X → 桩化旧读。
     */
    private void stubInvalidatedReads(List<Item> items, Set<Integer> protectedIdx) {
        for (Item item : items) {
            if (item.disposition != Disposition.LIVE || protectedIdx.contains(item.index)) {
                continue;
            }
            ToolClass tc = item.toolClass;
            if (tc == null || tc.kind() != ToolKind.SNAPSHOT || StringUtils.isBlank(tc.resourceKey())) {
                continue;
            }
            boolean invalidated = false;
            for (int j = item.index + 1; j < items.size(); j++) {
                ToolClass later = items.get(j).toolClass;
                if (later == null) {
                    continue;
                }
                if (tc.resourceKey().equals(later.writesKey()) || tc.resourceKey().equals(later.resourceKey())) {
                    invalidated = true;
                    break;
                }
            }
            if (invalidated) {
                item.disposition = Disposition.STUBBED;
                item.stubText = "[结果已省略: " + tc.resourceKey() + " 已被后续操作覆盖]";
            }
        }
    }

    /**
     * 序 1c：大参数桩——旧的大参数写操作被后文对同一目标的写覆盖 → 换参数桩。
     */
    private void stubSupersededArguments(List<Item> items, Set<Integer> protectedIdx) {
        for (Item item : items) {
            if (item.disposition != Disposition.LIVE || protectedIdx.contains(item.index)) {
                continue;
            }
            ToolClass tc = item.toolClass;
            if (tc == null || StringUtils.isBlank(tc.writesKey())) {
                continue;
            }
            String arguments = item.content.getString("functionArguments", "");
            if (arguments.length() <= BIG_ARGUMENT_CHARS) {
                continue;
            }
            boolean superseded = false;
            for (int j = item.index + 1; j < items.size(); j++) {
                ToolClass later = items.get(j).toolClass;
                if (later != null && tc.writesKey().equals(later.writesKey())) {
                    superseded = true;
                    break;
                }
            }
            if (superseded) {
                item.disposition = Disposition.ARG_STUB;
                item.stubArguments = stubArguments(arguments);
            }
        }
    }

    /**
     * 序 2：同工具名 + 同参数重复调用，只留最后一次。
     */
    private void stubDuplicates(List<Item> items, Set<Integer> protectedIdx) {
        Map<String, List<Item>> groups = new LinkedHashMap<>();
        for (Item item : items) {
            if (item.type != ContentTypeConstants.TOOL || item.disposition != Disposition.LIVE) {
                continue;
            }
            String key = item.content.getString("toolName", "")
                    + " " + item.content.getString("functionArguments", "");
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }
        for (List<Item> group : groups.values()) {
            for (int i = 0; i < group.size() - 1; i++) {
                Item item = group.get(i);
                if (!protectedIdx.contains(item.index)) {
                    item.disposition = Disposition.STUBBED;
                    item.stubText = "[重复调用已省略，最新结果见后文]";
                }
            }
        }
    }

    /**
     * 序 1b：年龄降级——远离末尾的快照 / 结论留首行；很长的截头尾。
     */
    private void degradeByAge(List<Item> items, Set<Integer> protectedIdx, Config cfg) {
        int last = items.size() - 1;
        for (Item item : items) {
            if (item.type != ContentTypeConstants.TOOL || item.disposition != Disposition.LIVE
                    || protectedIdx.contains(item.index)) {
                continue;
            }
            ToolClass tc = item.toolClass;
            if (tc == null) {
                continue;
            }
            int distance = last - item.index;
            String content = item.content.getString("content", "");
            boolean stubKind = tc.kind() == ToolKind.SNAPSHOT || tc.kind() == ToolKind.CONCLUSION;
            if (stubKind && distance >= cfg.stubAge()) {
                item.disposition = Disposition.STUBBED;
                item.stubText = firstLine(content, 120) + "\n…[早期结果已省略]";
            } else if (content.length() > LONG_CONTENT_CHARS && distance >= cfg.truncAge()) {
                item.disposition = Disposition.TRUNCATED;
                item.stubText = content.substring(0, 600)
                        + "\n…[中间 " + (content.length() - 900) + " 字已省略]…\n"
                        + content.substring(content.length() - 300);
            }
        }
    }

    // ══════════════════════════ 摘要游标推进（增量滚动） ══════════════════════════

    private record DigestResult(String summary, int covered, boolean llm, List<Fact> facts, String error) {
    }

    /**
     * 从 covered 起连续向前摘（跳过保持原文的 SYSTEM / 首条任务，吸收路径上的旧 digest），
     * 压到低水位即停；LLM 摘要器只对本次新增段调一次（输入 = 当前摘要 + 段落原文），失败回退抽取式。
     */
    private DigestResult digestForward(List<Item> items, Config cfg, int base, int low,
                                       String summary, int covered, int firstQuestionIdx) {
        int keepFrom = Math.max(covered, items.size() - cfg.keepRecentItems());
        StringBuilder lines = new StringBuilder();
        // 去重集合用已有摘要的行预填充：跨 run 时 covered 归零、上一轮 run 的消息作为历史
        // 重新载入会被重摘一遍，行级去重保证同样内容不会在摘要里追加第二次
        Set<String> lineSet = new LinkedHashSet<>();
        for (String existing : summary.split("\n")) {
            String stripped = existing.strip();
            if (stripped.startsWith("- ")) {
                stripped = stripped.substring(2).strip();
            }
            if (!stripped.isEmpty()) {
                lineSet.add(stripped);
            }
        }
        StringBuilder segmentRaw = new StringBuilder();
        boolean digestedLossy = false;

        int i = covered;
        while (i < keepFrom) {
            Item item = items.get(i);
            if (item.type == ContentTypeConstants.SYSTEM || i == firstQuestionIdx) {
                i++;
                covered = i;
                continue;
            }
            if (item.digest) {
                summary = appendSummary(summary, stripDigestHeader(item.content.getString("content", "")));
                item.disposition = Disposition.SUMMARIZED;
                i++;
                covered = i;
                continue;
            }
            if (digestedLossy && effective(items, base)
                    + tokenCounter.count(summary + "\n" + lines) <= low) {
                break;
            }
            if (item.disposition != Disposition.FOLDED) {
                String line = digestItem(item);
                // 去重：同样的失败/产出在一批里只留一条
                if (StringUtils.isNotBlank(line) && lineSet.add(line)) {
                    lines.append("- ").append(line).append("\n");
                }
                String raw = rawItemText(item);
                if (StringUtils.isNotBlank(raw) && segmentRaw.length() < SEGMENT_INPUT_MAX_CHARS) {
                    segmentRaw.append(raw).append("\n");
                }
                digestedLossy = true;
            }
            item.disposition = Disposition.SUMMARIZED;
            i++;
            covered = i;
        }

        if (!digestedLossy && lines.isEmpty()) {
            return new DigestResult(capSummary(summary), covered, false, List.of(), null);
        }

        // 抽取式兜底结果先备好，LLM 成功则整体替换（输入 = 当前摘要 + 新增段原文，增量滚动）
        String merged = appendSummary(summary, lines.toString().strip());
        boolean llm = false;
        String error = null;
        List<Fact> llmFacts = List.of();
        if (summarizer != null && digestedLossy) {
            try {
                SummarizedNotes result = summarizer.summarize(summary,
                        StringUtils.abbreviate(segmentRaw.toString().strip(), SEGMENT_INPUT_MAX_CHARS),
                        cfg.sections());
                if (result != null && StringUtils.isNotBlank(result.summary())) {
                    merged = result.summary().strip();
                    llm = true;
                    if (result.facts() != null) {
                        // 占位示例行过滤 + 只收启用子区
                        Set<String> allowedSubtypes = new HashSet<>();
                        for (SectionDef sd : cfg.sections()) {
                            if (sd != null && StringUtils.isNotBlank(sd.key())) {
                                allowedSubtypes.add(sd.key());
                            }
                        }
                        llmFacts = result.facts().stream()
                                .filter(f -> allowedSubtypes.contains(f.subtype()))
                                .filter(f -> StringUtils.isNotBlank(f.key()) && StringUtils.isNotBlank(f.value()))
                                .filter(f -> !isPlaceholderValue(f.value()))
                                .toList();
                    }
                }
            } catch (Exception e) {
                // 摘要器失败 → 回退抽取式，不阻断；原因随 stats 透出，避免只能翻日志诊断
                error = e.toString();
            }
        }
        return new DigestResult(capSummary(merged), covered, llm, llmFacts, error);
    }

    /**
     * 抽取式摘要行：只保留有记忆价值的条目——用户问题、非空助手回答、失败、成功的文件产出；
     * 读/探测类工具调用是过程细节，不进摘要（流水账噪音）。
     */
    private String digestItem(Item item) {
        return switch (item.type) {
            case QUESTION -> {
                String line = firstLine(item.content.getString("content", ""), 100);
                yield StringUtils.isBlank(line) ? "" : "用户: " + line;
            }
            case TEXT -> {
                String line = firstLine(item.content.getString("content", ""), 100);
                yield StringUtils.isBlank(line) ? "" : "助手: " + line;
            }
            case TOOL -> {
                ToolClass tc = item.toolClass;
                if (tc == null) {
                    yield "";
                }
                if (Boolean.FALSE.equals(tc.ok())) {
                    yield "工具 " + item.content.getString("toolName", "?") + " 执行失败";
                }
                if (tc.kind() == ToolKind.MILESTONE && Boolean.TRUE.equals(tc.ok())) {
                    if (tc.deletesKey() != null && tc.deletesKey().startsWith("file:")) {
                        yield "删除文件: " + tc.deletesKey().substring("file:".length());
                    }
                    if (tc.writesKey() != null && tc.writesKey().startsWith("file:")) {
                        yield "产出/更新文件: " + tc.writesKey().substring("file:".length());
                    }
                }
                yield "";
            }
            default -> "";
        };
    }

    /**
     * 待摘条目的原文（供 LLM 摘要输入；单条限长防爆，空内容跳过）
     */
    private String rawItemText(Item item) {
        return switch (item.type) {
            case QUESTION, TEXT -> {
                String content = item.content.getString("content", "");
                if (StringUtils.isBlank(content)) {
                    yield "";
                }
                yield (item.type == ContentTypeConstants.QUESTION ? "用户: " : "助手: ")
                        + StringUtils.abbreviate(content, 1500);
            }
            case TOOL -> "工具 " + item.content.getString("toolName", "?")
                    + " 参数: " + StringUtils.abbreviate(item.content.getString("functionArguments", ""), 300)
                    + " 结果: " + StringUtils.abbreviate(item.content.getString("content", ""), 800);
            default -> "";
        };
    }

    // ══════════════════════════ 产物 ══════════════════════════

    /**
     * 产物登记：按时序推进状态——成功的文件写 → active，成功的 rm → deleted（删了又重建则回到 active）。
     * deleted 便签照常写出落库（渲染时隐藏），避免下一轮 merge 时旧的 active 复活。
     */
    private List<Fact> collectArtifactFacts(List<Item> items) {
        Map<String, String> status = new LinkedHashMap<>();
        for (Item item : items) {
            if (item.type != ContentTypeConstants.TOOL || item.toolClass == null) {
                continue;
            }
            ToolClass tc = item.toolClass;
            if (tc.kind() != ToolKind.MILESTONE || !Boolean.TRUE.equals(tc.ok())) {
                continue;
            }
            if (tc.deletesKey() != null && tc.deletesKey().startsWith("file:")) {
                String path = tc.deletesKey().substring("file:".length());
                if (StringUtils.isNotBlank(path)) {
                    status.put(path, "deleted");
                }
            } else if (tc.writesKey() != null && tc.writesKey().startsWith("file:")) {
                String path = tc.writesKey().substring("file:".length());
                if (StringUtils.isNotBlank(path)) {
                    status.put(path, "active");
                }
            }
        }
        return status.entrySet().stream()
                .map(e -> new Fact(ARTIFACT_SUBTYPE, e.getKey(), e.getValue()))
                .toList();
    }

    // ══════════════════════════ 渲染 ══════════════════════════

    /**
     * 便签 / 产物旁注：内置固定顺序（cache 友好）[目标]→[喜好]→[约定]→[环境]→[待办] →
     * 自定义子区（按配置顺序，标题用 label）→ 兜底渲染任意残余子区（标题回退 key）→ [产物]。
     * 占位示例行（值以「如」开头）过滤。
     */
    private String renderNotes(List<Fact> facts, List<SectionDef> renderSections) {
        StringBuilder sb = new StringBuilder();
        // 标题 / 顺序 / 列表型全部来自便签设置（renderSections 已按 sortOrder 排序），不查硬编码表
        Set<String> rendered = new HashSet<>();
        if (renderSections != null) {
            for (SectionDef sd : renderSections) {
                if (sd != null && StringUtils.isNotBlank(sd.key()) && rendered.add(sd.key())) {
                    appendSection(sb, facts, sd.key(), StringUtils.defaultIfBlank(sd.label(), sd.key()),
                            sd.listStyle());
                }
            }
        }
        // 兜底：抽到了但不在便签设置里、非产物的 subtype —— 用 key 作标题渲染，避免"抽到却不显示"
        for (String section : facts.stream().map(Fact::subtype)
                .filter(s -> StringUtils.isNotBlank(s) && !ARTIFACT_SUBTYPE.equals(s))
                .distinct().toList()) {
            if (rendered.add(section)) {
                appendSection(sb, facts, section, section, false);
            }
        }
        // 产物固定渲染（不受 factSections 控制）：key = 路径，value = 状态；已删除的不渲染（记录保留在库）
        List<Fact> artifactFacts = facts.stream()
                .filter(f -> ARTIFACT_SUBTYPE.equals(f.subtype()))
                .filter(f -> StringUtils.isNotBlank(f.key()))
                .filter(f -> !"deleted".equals(f.value()))
                .toList();
        if (!artifactFacts.isEmpty()) {
            sb.append("[产物]\n");
            for (Fact fact : artifactFacts) {
                sb.append("- ").append(fact.key())
                        .append(" (").append(StringUtils.defaultIfBlank(fact.value(), "active")).append(")\n");
            }
        }
        return sb.toString().strip();
    }

    /**
     * 渲染单个子区：过滤空/占位值，非空则输出 [标题] + 每条 `- 键: 值`
     */
    private void appendSection(StringBuilder sb, List<Fact> facts, String subtype, String title, boolean listStyle) {
        List<Fact> sectionFacts = facts.stream()
                .filter(f -> subtype.equals(f.subtype()))
                .filter(f -> StringUtils.isNotBlank(f.value()) && !isPlaceholderValue(f.value()))
                .toList();
        if (sectionFacts.isEmpty()) {
            return;
        }
        // 按归一化 value 去重（去空白/标点后比较）：抽取器每轮换措辞会堆近似重复，这里保留较新的一条。
        LinkedHashMap<String, Fact> unique = new LinkedHashMap<>();
        for (Fact f : sectionFacts) {
            unique.put(normalizeForDedup(f.value()), f);
        }
        List<Fact> deduped = new ArrayList<>(unique.values());
        // 列表型子区（便签设置 list_style=true，如目标/待办）随对话演进、无稳定身份，只保留最近 N 条，避免无限堆积。
        if (listStyle && deduped.size() > LIST_SECTION_MAX) {
            deduped = deduped.subList(deduped.size() - LIST_SECTION_MAX, deduped.size());
        }
        sb.append("[").append(title).append("]\n");
        for (Fact fact : deduped) {
            String key = StringUtils.defaultString(fact.key()).strip();
            String value = fact.value().strip();
            // 列表型便签抽取器常把整句同时塞进 key 与 value；key 空或与 value 相同时
            // 只渲染 "- 值"，避免 "- X: X" 的重复噪音（也省 token）。
            if (key.isEmpty() || key.equals(value)) {
                sb.append("- ").append(value).append("\n");
            } else {
                sb.append("- ").append(key).append(": ").append(value).append("\n");
            }
        }
    }

    /** 列表型子区渲染上限：只保留最近 N 条 */
    private static final int LIST_SECTION_MAX = 6;

    /** 归一化 value 用于去重：去掉空白与标点（含全角/CJK），转小写 */
    private static String normalizeForDedup(String value) {
        return value.strip().replaceAll("[\\s\\p{P}]+", "").toLowerCase(Locale.ROOT);
    }

    /**
     * 摘要消息的插入位置：第一个被摘条目处（时序正确）；没有被摘条目则最前
     */
    private static int digestInsertIndex(List<Item> items) {
        for (Item item : items) {
            if (item.disposition == Disposition.SUMMARIZED) {
                return item.index;
            }
        }
        return 0;
    }

    private List<Map<String, Object>> renderCompressed(List<Item> items, String digestText, int digestInsertIndex) {
        List<Map<String, Object>> result = new ArrayList<>();
        boolean digestEmitted = digestText == null;
        for (Item item : items) {
            if (!digestEmitted && item.index >= digestInsertIndex) {
                // 摘要以 assistant(TEXT) 呈现：折叠掉助手回合后留下 user—user 空档，
                // 单条相反角色正好补成交替；内容本就是助手对既往工作的复述，口吻也贴切。
                result.add(new JsonObject()
                        .put("type", ContentTypeConstants.TEXT.name())
                        .put("content", digestText)
                        .getMap());
                digestEmitted = true;
            }
            switch (item.disposition) {
                case FOLDED, SUMMARIZED -> {
                }
                case LIVE -> result.add(item.content.getMap());
                case STUBBED, TRUNCATED -> {
                    JsonObject copy = item.content.copy();
                    copy.put("content", item.stubText);
                    result.add(copy.getMap());
                }
                case ARG_STUB -> {
                    JsonObject copy = item.content.copy();
                    copy.put("functionArguments", item.stubArguments);
                    result.add(copy.getMap());
                }
            }
        }
        if (!digestEmitted) {
            result.add(new JsonObject()
                    .put("type", ContentTypeConstants.TEXT.name())
                    .put("content", digestText)
                    .getMap());
        }
        return result;
    }

    /**
     * 严格 user/assistant 交替收口：合并相邻同类型消息，保证投影里不出现连续同角色。
     * 压缩会折叠掉部分助手回合，使保留的用户问题相邻（如 首个问题 + 历史摘要 + 最新问题 都是 QUESTION），
     * 严格交替的 chat 模型会拒绝；这里把相邻同类型的 content 以空行拼接成一条。首部单条 SYSTEM 便签自然保留。
     */
    public static List<Map<String, Object>> enforceAlternation(List<Map<String, Object>> messages) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> msg : messages) {
            String type = Objects.toString(msg.get("type"), "");
            // 只合并纯文本类角色（SYSTEM/QUESTION/TEXT）；TOOL 带 toolName/functionArguments 等结构化字段、
            // 还要与某次 assistant tool_call 配对，拼接 content 会丢参数、乱配对，绝不合并。
            boolean mergeable = "SYSTEM".equals(type) || "QUESTION".equals(type) || "TEXT".equals(type);
            if (mergeable && !out.isEmpty()
                    && type.equals(Objects.toString(out.get(out.size() - 1).get("type"), ""))) {
                Map<String, Object> last = out.get(out.size() - 1);
                last.put("content", (Objects.toString(last.get("content"), "")
                        + "\n\n" + Objects.toString(msg.get("content"), "")).strip());
                continue;
            }
            out.add(new LinkedHashMap<>(msg));
        }
        return out;
    }

    // ══════════════════════════ token ══════════════════════════

    private int countItem(JsonObject content, ContentTypeConstants type) {
        if (type == ContentTypeConstants.TOOL) {
            return tokenCounter.count(content.getString("toolName", ""))
                    + tokenCounter.count(content.getString("functionArguments", ""))
                    + tokenCounter.count(content.getString("content", ""))
                    + 8;
        }
        return tokenCounter.count(content.getString("content", "")) + 4;
    }

    private int effectiveTokens(Item item) {
        return switch (item.disposition) {
            case FOLDED, SUMMARIZED -> 0;
            case LIVE -> item.tokens;
            case STUBBED, TRUNCATED -> tokenCounter.count(item.stubText) + 8;
            case ARG_STUB -> tokenCounter.count(item.stubArguments)
                    + tokenCounter.count(item.content.getString("content", "")) + 8;
        };
    }

    private int effective(List<Item> items, int base) {
        return base + items.stream().mapToInt(this::effectiveTokens).sum();
    }

    // ══════════════════════════ 工具方法 ══════════════════════════

    /**
     * 占位示例行：LLM 照抄提示词示例时值形如「如: xxx」「如 xxx」。
     * 只匹配「如」后跟分隔符，不误伤「如何/如果」开头的正常内容（目标锚点常见）
     */
    private static boolean isPlaceholderValue(String value) {
        if (value.length() < 2 || value.charAt(0) != '如') {
            return false;
        }
        char next = value.charAt(1);
        return next == ':' || next == '：' || Character.isWhitespace(next);
    }

    private static String appendSummary(String summary, String appended) {
        if (StringUtils.isBlank(appended)) {
            return summary;
        }
        return StringUtils.isBlank(summary) ? appended : summary + "\n" + appended;
    }

    private static String capSummary(String summary) {
        if (summary.length() <= DIGEST_MAX_CHARS) {
            return summary;
        }
        // 截断对齐到行边界，避免留半行
        int cut = summary.length() - DIGEST_MAX_CHARS;
        int newline = summary.indexOf('\n', cut);
        String tail = newline >= 0 && newline + 1 < summary.length()
                ? summary.substring(newline + 1)
                : summary.substring(cut);
        return "…（更早摘要已截断）\n" + tail;
    }

    private static String stripDigestHeader(String digestContent) {
        int newline = digestContent.indexOf('\n');
        return (newline >= 0 ? digestContent.substring(newline + 1) : digestContent).strip();
    }

    private String stubArguments(String arguments) {
        try {
            JsonObject parsed = new JsonObject(arguments);
            JsonObject stub = new JsonObject();
            if (parsed.containsKey("path")) {
                stub.put("path", parsed.getValue("path"));
            }
            stub.put("_omitted", "[已省略 " + arguments.length() + " 字参数，该目标已被后续操作覆盖]");
            return stub.encode();
        } catch (Exception e) {
            return new JsonObject()
                    .put("_omitted", "[已省略 " + arguments.length() + " 字参数]")
                    .encode();
        }
    }

    private static ContentTypeConstants parseType(JsonObject content) {
        String type = content.getString("type");
        if (StringUtils.isBlank(type) || !ContentTypeConstants.contains(type)) {
            return null;
        }
        return ContentTypeConstants.valueOf(type);
    }

    private static boolean hasFiles(JsonObject question) {
        return isNotEmptyArray(question, "images")
                || isNotEmptyArray(question, "videos")
                || isNotEmptyArray(question, "files");
    }

    private static boolean isNotEmptyArray(JsonObject obj, String key) {
        Object value = obj.getValue(key);
        if (value instanceof io.vertx.core.json.JsonArray ja) {
            return !ja.isEmpty();
        }
        if (value instanceof List<?> list) {
            return !list.isEmpty();
        }
        return value != null;
    }

    private static String firstLine(String text, int maxChars) {
        if (StringUtils.isBlank(text)) {
            return "";
        }
        String line = text.strip();
        int newline = line.indexOf('\n');
        if (newline > 0) {
            line = line.substring(0, newline);
        }
        return StringUtils.abbreviate(line, maxChars);
    }
}
