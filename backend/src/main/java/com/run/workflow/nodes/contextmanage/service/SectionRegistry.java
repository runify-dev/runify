package com.run.workflow.nodes.contextmanage.service;

import com.run.RunApplication;
import com.run.dao.entity.CtxFact;
import com.run.dao.entity.CtxSection;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.run.sql.DSL.field;

/**
 * 便签子区注册表：应用级配置的单一真相源（"便签设置"菜单）。
 * <p>
 * 定义有哪些子区、显示标题、抽取说明，以及每类便签的归属 scope（user/conversation/application）。
 * 应用未配置（表内无该 application 的行）时回退 {@link #BUILTIN_DEFAULTS}——保证存量应用零改动照常工作。
 * <p>
 * save 节点据 scope 决定便签落哪个 scope；抽取/渲染据 label/description/listStyle 取用。
 */
public final class SectionRegistry {

    private static final long DB_TIMEOUT_SECONDS = 10;

    public record Section(String key, String label, String description,
                          String scope, boolean listStyle, boolean enabled, int sortOrder) {
    }

    /**
     * 内置默认（应用未配置时生效）：约定/环境→application，喜好→user，目标/待办→conversation。
     * scope 即"这个便签跟谁走"，用户可在便签设置里改。
     */
    public static final List<Section> BUILTIN_DEFAULTS = List.of(
            new Section("convention", "约定", "项目约定：规则/规范/禁忌（面向任务本身，非个人）",
                    "application", false, true, 10),
            new Section("preference", "喜好", "用户个人喜好：称呼/语言/语气/风格倾向/口味（跟人走）",
                    "user", false, true, 20),
            new Section("env", "环境", "环境事实：依赖/版本/路径/配置",
                    "application", false, true, 30),
            new Section("goal", "目标", "任务目标/验收标准/约束",
                    "conversation", true, true, 40),
            new Section("todo", "待办", "未完成事项/下一步",
                    "conversation", true, true, 50));

    private SectionRegistry() {
    }

    /**
     * 载入某应用的子区配置（按 sortOrder 排序）；无配置或应用 id 为空 → 内置默认。
     */
    public static List<Section> load(String applicationId) {
        if (StringUtils.isBlank(applicationId)) {
            return BUILTIN_DEFAULTS;
        }
        return fromRows(await(RunApplication.appComponent.ctxSectionMapper().list(
                field(CtxSection::getApplicationId).eq(applicationId), Map.of())));
    }

    /**
     * 行 → Section（纯函数，供阻塞的 {@link #load} 与异步的路由处理器共用）；空则内置默认。
     */
    public static List<Section> fromRows(List<CtxSection> rows) {
        if (rows == null || rows.isEmpty()) {
            return BUILTIN_DEFAULTS;
        }
        return rows.stream()
                .map(r -> new Section(
                        r.getSectionKey(),
                        StringUtils.defaultString(r.getLabel(), r.getSectionKey()),
                        StringUtils.defaultString(r.getDescription()),
                        StringUtils.defaultIfBlank(r.getScope(), "conversation"),
                        Boolean.TRUE.equals(r.getListStyle()),
                        !Boolean.FALSE.equals(r.getEnabled()),
                        r.getSortOrder() == null ? 0 : r.getSortOrder()))
                .sorted(Comparator.comparingInt(Section::sortOrder))
                .toList();
    }

    /**
     * subtype → scope 映射（save 节点路由用）。未列出的 subtype 由调用方兜底 conversation。
     */
    public static Map<String, String> scopeMap(String applicationId) {
        Map<String, String> map = new LinkedHashMap<>();
        for (Section s : load(applicationId)) {
            if (StringUtils.isNotBlank(s.key())) {
                map.put(s.key(), s.scope());
            }
        }
        return map;
    }

    /**
     * 渲染「我的便签」只读视图（供 handler 直出 JSON，供终端/后台两个入口共用，保证结构一致）：
     * 过滤观察期(provisional)与空值，附子区标题 label（缺省回退 key），按子区 sortOrder → subtype 排序。
     * sectionRows 传应用便签配置行（空则内置默认）；facts 传目标 scope 的便签行。
     */
    public static JsonArray renderUserFacts(List<CtxSection> sectionRows, List<CtxFact> facts) {
        Map<String, Section> byKey = new LinkedHashMap<>();
        for (Section s : fromRows(sectionRows)) {
            byKey.put(s.key(), s);
        }
        JsonArray arr = new JsonArray();
        if (facts == null) {
            return arr;
        }
        facts.stream()
                .filter(f -> !Boolean.TRUE.equals(f.getProvisional()))
                .filter(f -> StringUtils.isNotBlank(f.getFactValue()))
                .sorted(Comparator.<CtxFact>comparingInt(f -> {
                            Section s = byKey.get(f.getSubtype());
                            return s == null ? Integer.MAX_VALUE : s.sortOrder();
                        })
                        .thenComparing(f -> StringUtils.defaultString(f.getSubtype())))
                .forEach(f -> {
                    Section s = byKey.get(f.getSubtype());
                    arr.add(new JsonObject()
                            .put("subtype", f.getSubtype())
                            .put("label", s != null ? s.label() : f.getSubtype())
                            .put("key", f.getFactKey())
                            .put("value", f.getFactValue())
                            .put("updateTime", Objects.toString(f.getUpdateTime(), null)));
                });
        return arr;
    }

    private static <T> T await(Future<T> future) {
        try {
            return future.toCompletionStage().toCompletableFuture()
                    .get(DB_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
