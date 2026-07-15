package com.run.workflow.nodes.contextmanage.service;

import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TOOL Content 分类器（判定瀑布）：
 * <ol>
 *     <li>内置规则：按 toolName 映射；命令类工具（run_command）再看 command 内容</li>
 *     <li>保守默认：未知工具 → MILESTONE（永不折叠）</li>
 * </ol>
 * 成败 ok：status 字段最权威（SUCCESS→true / FAIL→false / 其余→null），无 status 时看内容正则。
 */
public final class ContentClassifier {

    public enum ToolKind {
        /**
         * 探测类：pwd / which / --version 等，结论可蒸馏进 env 便签后折叠
         */
        PROBE,
        /**
         * 快照类：read_file / list_dir / grep 等读操作，可被后续写覆盖失效
         */
        SNAPSHOT,
        /**
         * 结论类：脚本执行输出等，年龄大了留首行
         */
        CONCLUSION,
        /**
         * 里程碑类：写操作 / 未知工具，永不折叠
         */
        MILESTONE
    }

    public record ToolClass(ToolKind kind, Boolean ok, String resourceKey, String writesKey, String deletesKey) {
        public ToolClass(ToolKind kind, Boolean ok, String resourceKey, String writesKey) {
            this(kind, ok, resourceKey, writesKey, null);
        }
    }

    private static final Set<String> PROBE_COMMANDS = Set.of(
            "pwd", "whoami", "which", "ls", "echo", "uname", "date", "env", "printenv", "type", "command", "wc", "du", "df", "file", "stat"
    );
    private static final Set<String> MILESTONE_COMMANDS = Set.of(
            "mkdir", "rm", "mv", "cp", "touch", "git", "pip", "pip3", "npm", "pnpm", "yarn",
            "apt", "apt-get", "brew", "chmod", "chown", "tar", "unzip", "zip", "curl", "wget",
            "make", "mvn", "gradle", "docker", "ln", "install"
    );

    private static final Pattern FAIL_CONTENT = Pattern.compile(
            "(?m)^\\s*(Traceback \\(most recent call last\\)|Error:|ERROR[: ]|错误[:：])");

    private ContentClassifier() {
    }

    /**
     * 对一条 TOOL Content 分类。
     *
     * @param tool TOOL 元素（toolName / functionArguments / content / status）
     */
    public static ToolClass classify(JsonObject tool) {
        String toolName = tool.getString("toolName", "");
        JsonObject args = parseArgs(tool.getString("functionArguments"));
        Boolean ok = resolveOk(tool);

        return switch (toolName) {
            case "read_file" -> new ToolClass(ToolKind.SNAPSHOT, ok, "file:" + args.getString("path", ""), null);
            case "list_dir" -> new ToolClass(ToolKind.SNAPSHOT, ok, "dir:" + args.getString("path", ""), null);
            case "glob" -> new ToolClass(ToolKind.SNAPSHOT, ok,
                    "glob:" + args.getString("pattern", "") + ":" + args.getString("path", ""), null);
            case "grep" -> new ToolClass(ToolKind.SNAPSHOT, ok,
                    "grep:" + args.getString("pattern", "") + ":" + args.getString("path", ""), null);
            case "list_skills" -> new ToolClass(ToolKind.SNAPSHOT, ok, "skills:", null);
            case "cache_query" -> new ToolClass(ToolKind.SNAPSHOT, ok, "cache:" + args.getString("key", ""), null);
            case "create_file" -> new ToolClass(ToolKind.MILESTONE, ok, null, key("file:", args.getString("path")));
            case "apply_patch" -> new ToolClass(ToolKind.MILESTONE, ok, null, patchTargetKey(args));
            case "file_upload", "file_download" ->
                    new ToolClass(ToolKind.MILESTONE, ok, null, key("file:", args.getString("path")));
            case "cache_write" -> new ToolClass(ToolKind.MILESTONE, ok, null, key("cache:", args.getString("key")));
            case "download_skills" -> new ToolClass(ToolKind.MILESTONE, ok, null, "skills:");
            case "run_skill" -> new ToolClass(ToolKind.CONCLUSION, ok, null, null);
            case "run_command", "terminal", "run_terminal" -> classifyCommand(args, ok);
            default -> new ToolClass(ToolKind.MILESTONE, ok, null, null);
        };
    }

    /**
     * 命令类工具：先剥掉 2>/dev/null、2>&1、>/dev/null 再判定，
     * 避免里程碑判定的裸 > 误命中重定向噪音。
     */
    private static ToolClass classifyCommand(JsonObject args, Boolean ok) {
        String command = firstNonEmpty(args.getString("command"), args.getString("code"), args.getString("cmd"));
        if (StringUtils.isBlank(command)) {
            return new ToolClass(ToolKind.CONCLUSION, ok, null, null);
        }
        String stripped = command
                .replace("2>/dev/null", " ")
                .replace("2> /dev/null", " ")
                .replace("2>&1", " ")
                .replace(">/dev/null", " ")
                .replace("> /dev/null", " ")
                .trim();

        String lower = stripped.toLowerCase(Locale.ROOT);
        if (lower.contains("--version") || lower.contains("--help") || lower.matches(".*\\s-v(ersion)?\\b.*")) {
            return new ToolClass(ToolKind.PROBE, ok, "cmd:" + normalize(stripped), null);
        }

        String firstWord = firstWord(stripped);
        if ("rm".equals(firstWord)) {
            String path = singleRmTarget(stripped);
            if (path != null) {
                // writesKey 让失效键规则桩化对该文件的旧读；deletesKey 供产物登记识别为删除
                return new ToolClass(ToolKind.MILESTONE, ok, null, "file:" + path, "file:" + path);
            }
            return new ToolClass(ToolKind.MILESTONE, ok, null, null);
        }
        if (MILESTONE_COMMANDS.contains(firstWord) || stripped.contains(">") || stripped.contains("<<")) {
            return new ToolClass(ToolKind.MILESTONE, ok, null, null);
        }
        if (PROBE_COMMANDS.contains(firstWord)) {
            return new ToolClass(ToolKind.PROBE, ok, "cmd:" + normalize(stripped), null);
        }
        if ("cat".equals(firstWord) || "head".equals(firstWord) || "tail".equals(firstWord)) {
            return new ToolClass(ToolKind.SNAPSHOT, ok, "cmd:" + normalize(stripped), null);
        }
        return new ToolClass(ToolKind.CONCLUSION, ok, null, null);
    }

    /**
     * 空值安全的资源键：路径/键为空时返回 null，避免产生 "file:" 空键
     * （空 writesKey 会让 digest 行出现空尾巴、也会让不同目标的写被误判为同一目标）
     */
    private static String key(String prefix, String value) {
        return StringUtils.isBlank(value) ? null : prefix + value;
    }

    private static final Pattern PATCH_NEW_FILE = Pattern.compile("(?m)^\\+\\+\\+ +b/(\\S+)");
    private static final Pattern PATCH_OLD_FILE = Pattern.compile("(?m)^--- +a/(\\S+)");

    /**
     * apply_patch 目标文件：path 参数经常缺失或是工作目录（"."），此时从补丁头行提取
     */
    private static String patchTargetKey(JsonObject args) {
        String path = args.getString("path", "");
        if (StringUtils.isNotBlank(path) && !".".equals(path)) {
            return "file:" + path;
        }
        String patch = args.getString("patch", "");
        Matcher matcher = PATCH_NEW_FILE.matcher(patch);
        if (matcher.find()) {
            return "file:" + matcher.group(1);
        }
        matcher = PATCH_OLD_FILE.matcher(patch);
        if (matcher.find()) {
            return "file:" + matcher.group(1);
        }
        return null;
    }

    /**
     * rm 的单一目标路径：多目标 / 通配符 / 复合命令太含糊，返回 null 按普通里程碑处理
     */
    private static String singleRmTarget(String command) {
        String[] tokens = command.trim().split("\\s+");
        String target = null;
        for (int i = 1; i < tokens.length; i++) {
            String token = tokens[i];
            if (token.startsWith("-")) {
                continue;
            }
            if (target != null) {
                return null;
            }
            target = token;
        }
        if (target == null) {
            return null;
        }
        target = target.replaceAll("^[\"']|[\"']$", "");
        if (target.contains("*") || target.contains("?") || StringUtils.isBlank(target)) {
            return null;
        }
        return target;
    }

    private static Boolean resolveOk(JsonObject tool) {
        Object status = tool.getValue("status");
        if (status != null) {
            String s = status.toString();
            if ("SUCCESS".equals(s)) {
                String content = tool.getString("content", "");
                // status 成功但内容里有硬错误信号时降级为失败
                return !FAIL_CONTENT.matcher(content).find();
            }
            if ("FAIL".equals(s)) {
                return Boolean.FALSE;
            }
            return null;
        }
        String content = tool.getString("content", "");
        if (StringUtils.isBlank(content)) {
            return null;
        }
        return !FAIL_CONTENT.matcher(content).find();
    }

    private static JsonObject parseArgs(String functionArguments) {
        if (StringUtils.isBlank(functionArguments)) {
            return new JsonObject();
        }
        try {
            return new JsonObject(functionArguments);
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    private static String firstWord(String command) {
        String[] parts = command.trim().split("\\s+", 2);
        return parts.length > 0 ? parts[0].toLowerCase(Locale.ROOT) : "";
    }

    private static String normalize(String command) {
        return StringUtils.abbreviate(command.trim().replaceAll("\\s+", " "), 128);
    }

    private static String firstNonEmpty(String... values) {
        for (String v : values) {
            if (StringUtils.isNotBlank(v)) {
                return v;
            }
        }
        return null;
    }
}
