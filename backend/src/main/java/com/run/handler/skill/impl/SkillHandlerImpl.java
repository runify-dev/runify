package com.run.handler.skill.impl;

import com.run.auth.constants.PermissionConstants;
import com.run.auth.dto.UserProfile;
import com.run.common.cache.CacheStore;
import com.run.common.result.Result;
import com.run.common.util.CommonUtils;
import com.run.dao.entity.Skill;
import com.run.dao.entity.SkillFile;
import com.run.dao.entity.SkillFolder;
import com.run.dao.entity.SkillPermission;
import com.run.dao.entity.SkillRelation;
import com.run.dao.mapper.FileMapper;
import com.run.dao.mapper.SkillFileMapper;
import com.run.dao.mapper.SkillFolderMapper;
import com.run.dao.mapper.SkillMapper;
import com.run.dao.mapper.SkillPermissionMapper;
import com.run.dao.mapper.SkillRelationMapper;
import com.run.handler.common.Tool;
import com.run.handler.common.impl.ResourceHandlerImpl;
import com.run.handler.common.pojo.SimpleNodePojo;
import com.run.common.util.TreeUtil;
import com.run.handler.skill.ISkillHandler;
import com.run.handler.skill.pojo.EditSkillPojo;
import org.apache.commons.lang3.StringUtils;
import com.run.sql.DSL;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import org.yaml.snakeyaml.Yaml;

import javax.inject.Inject;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class SkillHandlerImpl extends ResourceHandlerImpl<Skill, SkillFolder, SkillPermission, SkillRelation, SkillMapper, SkillFolderMapper, SkillPermissionMapper, SkillRelationMapper> implements ISkillHandler {

    private final SkillFileMapper skillFileMapper;
    private final FileMapper fileMapper;
    private final SkillMapper skillMapper;
    private final SkillRelationMapper skillRelationMapper;

    private static final UUID ROOT_FOLDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    // 厂商命名空间优先级：先认自家，再退回 OpenClaw 及其别名
    private static final List<String> VENDOR_NS = List.of("estellexn", "openclaw", "clawdbot", "clawdis");

    private static final Pattern FRONTMATTER =
            Pattern.compile("\\A---\\r?\\n(.*?)\\r?\\n---\\r?\\n?(.*)\\z", Pattern.DOTALL);

    // 敏感性判定（决定 PasswordInput vs TextInput）
    private static final Pattern SECRET_NAME = Pattern.compile(
            ".*(API[_-]?KEY|ACCESS[_-]?KEY|SECRET|TOKEN|PASSWORD|PASSWD|PWD|CREDENTIAL|AUTH|BEARER|PRIVATE[_-]?KEY).*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SECRET_DESC = Pattern.compile(
            "\\b(token|api[ -]?key|secret|credential|password|bearer|auth(?:entication|orization)?|access key)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NONSECRET_NAME = Pattern.compile(
            ".*(PUBLIC[_-]?KEY|_URL|_URI|_ENDPOINT|_HOST|_PORT|_REGION|_MODEL|_BASE|PROJECT[_-]?ID|_ID|_NAME)$",
            Pattern.CASE_INSENSITIVE);

    // 从正文/脚本推断 env 引用
    private static final Pattern[] ENV_INFER_PATTERNS = {
            Pattern.compile("process\\.env\\.([A-Z][A-Z0-9_]*)"),
            Pattern.compile("process\\.env\\[['\"]([A-Z][A-Z0-9_]*)['\"]\\]"),
            Pattern.compile("os\\.environ(?:\\.get)?\\(?\\[?['\"]([A-Z][A-Z0-9_]*)['\"]"),
            Pattern.compile("os\\.getenv\\(['\"]([A-Z][A-Z0-9_]*)['\"]"),
            Pattern.compile("System\\.getenv\\(['\"]([A-Z][A-Z0-9_]*)['\"]"),
            Pattern.compile("\\$\\{?([A-Z][A-Z0-9_]{2,})\\}?")
    };
    private static final Set<String> ENV_NOISE = Set.of(
            "PATH", "HOME", "PWD", "USER", "SHELL", "LANG", "TERM", "TMPDIR", "NODE_ENV", "PORT");

    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            ".md", ".markdown", ".txt", ".py", ".js", ".jsx", ".ts", ".tsx",
            ".sh", ".bash", ".sql", ".json", ".yaml", ".yml", ".xml", ".html",
            ".css", ".java", ".go", ".rs", ".c", ".cpp", ".h", ".rb", ".php",
            ".lua", ".r", ".toml", ".ini", ".env", ".dockerfile", ".makefile",
            ".csv", ".log", ".cfg", ".conf", ".properties"
    );

    @Inject
    public SkillHandlerImpl(SkillMapper skillMapper,
                            SkillFolderMapper skillFolderMapper,
                            SkillRelationMapper skillRelationMapper,
                            SkillPermissionMapper skillPermissionMapper,
                            CacheStore cacheStore,
                            SkillFileMapper skillFileMapper,
                            FileMapper fileMapper) {
        super(skillMapper, skillFolderMapper, skillRelationMapper, skillPermissionMapper, cacheStore);
        this.skillFileMapper = skillFileMapper;
        this.fileMapper = fileMapper;
        this.skillMapper = skillMapper;
        this.skillRelationMapper = skillRelationMapper;
    }

    private boolean isTextFile(String name) {
        String lower = name.toLowerCase();
        int dot = lower.lastIndexOf('.');
        if (dot > 0) {
            return TEXT_EXTENSIONS.contains(lower.substring(dot));
        }
        return false;
    }

    private String buildFilePath(Map<String, SkillFile> fileMap, SkillFile file) {
        List<String> parts = new ArrayList<>();
        parts.add(file.getName());
        UUID parentId = file.getParentId();
        while (parentId != null && fileMap.containsKey(parentId.toString())) {
            SkillFile parent = fileMap.get(parentId.toString());
            parts.add(0, parent.getName());
            parentId = parent.getParentId();
        }
        return String.join("/", parts);
    }

    @Override
    public void edit(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");
        EditSkillPojo pojo = context.body().asPojo(EditSkillPojo.class);
        skillMapper.getById(resourceId).compose(skill -> {
            if (StringUtils.isNotEmpty(pojo.getName())) skill.setName(pojo.getName());
            if (StringUtils.isNotEmpty(pojo.getIcon())) skill.setIcon(pojo.getIcon());
            if (pojo.getDesc() != null) skill.setDesc(pojo.getDesc());
            if (pojo.getParameterValue() != null) skill.setParameterValue(pojo.getParameterValue());
            if (pojo.getSkillParameterForm() != null) skill.setSkillParameterForm(pojo.getSkillParameterForm());
            skill.setUpdateTime(LocalDateTime.now());
            return skillMapper.update(skill);
        }).compose(_ -> skillMapper.getById(resourceId))
                .onSuccess(rs -> context.end(Result.success(rs).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void exportSkill(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");
        skillMapper.getById(resourceId)
                .compose(skill -> skillFileMapper.list(
                                DSL.field("skill_id").eq(DSL.param("skillId")),
                                Map.of("skillId", resourceId))
                        .map(files -> Map.entry(skill, files)))
                .onSuccess(entry -> {
                    Skill skill = entry.getKey();
                    List<SkillFile> files = entry.getValue();
                    Map<String, SkillFile> fileMap = new HashMap<>();
                    for (SkillFile f : files) {
                        fileMap.put(f.getId().toString(), f);
                    }
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ZipOutputStream zos = new ZipOutputStream(baos);

                        // 纯目录结构打包，不含 manifest / structure。
                        // 注意：parameterValue / env 是用户私有值，绝不导出——这里只打包文件树。
                        for (SkillFile file : files) {
                            String path = buildFilePath(fileMap, file);
                            if ("folder".equals(file.getType())) {
                                zos.putNextEntry(new ZipEntry(path + "/"));
                                zos.closeEntry();
                            } else if ("text".equals(file.getType())) {
                                zos.putNextEntry(new ZipEntry(path));
                                if (file.getContent() != null) {
                                    zos.write(file.getContent().getBytes(StandardCharsets.UTF_8));
                                }
                                zos.closeEntry();
                            } else if ("file".equals(file.getType())) {
                                // 二进制文件暂不支持直接导出，写空条目占位
                                zos.putNextEntry(new ZipEntry(path));
                                zos.closeEntry();
                            }
                        }

                        zos.finish();
                        zos.close();

                        byte[] zipBytes = baos.toByteArray();
                        String fileName = skill.getName() + ".zip";
                        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
                        context.response()
                                .putHeader("Content-Type", "application/zip")
                                .putHeader("Content-Disposition", "attachment;filename*=UTF-8''" + encoded)
                                .putHeader("Content-Length", String.valueOf(zipBytes.length))
                                .end(io.vertx.core.buffer.Buffer.buffer(zipBytes));
                    } catch (Exception e) {
                        context.fail(e);
                    }
                })
                .onFailure(context::fail);
    }

    @Override
    public void importSkill(RoutingContext context) {
        String folderId = context.pathParam("folderId");
        List<FileUpload> uploads = context.fileUploads();
        if (uploads.isEmpty()) {
            context.fail(400);
            return;
        }
        FileUpload upload = uploads.getFirst();

        try {
            Path tempDir = Files.createTempDirectory("skill-import-");
            // 1. 解压 ZIP（带 zip-slip 防护）
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(upload.uploadedFileName()))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path filePath = tempDir.resolve(entry.getName()).normalize();
                    if (!filePath.startsWith(tempDir)) {
                        zis.closeEntry();
                        continue;
                    }
                    if (entry.isDirectory()) {
                        Files.createDirectories(filePath);
                    } else {
                        Files.createDirectories(filePath.getParent());
                        Files.copy(zis, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                    zis.closeEntry();
                }
            }

            // 2. 定位内容根（兼容 zip -r folder 包了一层的情况）
            Path contentRoot = resolveContentRoot(tempDir);

            // 3. 扫描目录构建结构
            JsonArray structure = new JsonArray();
            buildStructureFromDir(contentRoot, ROOT_FOLDER_ID, structure, new HashMap<>());

            if (structure.isEmpty()) {
                deleteTempDir(tempDir);
                context.fail(new IllegalStateException("压缩包内未找到任何技能文件"));
                return;
            }

            UUID skillId = UUID.randomUUID();
            UUID parentUuId = TreeUtil.getParentUuId(folderId);
            LocalDateTime now = LocalDateTime.now();

            Map<String, UUID> idMap = new HashMap<>();
            for (int i = 0; i < structure.size(); i++) {
                idMap.put(structure.getJsonObject(i).getString("id"), UUID.randomUUID());
            }

            List<SkillFile> foldersToSave = new ArrayList<>();
            List<SkillFile> textsToSave = new ArrayList<>();
            List<Map.Entry<SkillFile, java.io.File>> filesToUpload = new ArrayList<>();

            for (int i = 0; i < structure.size(); i++) {
                JsonObject item = structure.getJsonObject(i);
                String type = item.getString("type");
                String oldParentId = item.getString("parentId", "");

                SkillFile sf = new SkillFile();
                sf.setId(idMap.get(item.getString("id")));
                sf.setParentId(oldParentId.isEmpty() ? ROOT_FOLDER_ID : idMap.getOrDefault(oldParentId, ROOT_FOLDER_ID));
                sf.setSkillId(skillId);
                sf.setName(item.getString("name"));
                sf.setType(type);
                sf.setCreateTime(now);
                sf.setUpdateTime(now);

                Path contentPath = contentRoot.resolve(buildFilePathFromStructure(structure, item));

                if ("text".equals(type)) {
                    sf.setContent(Files.exists(contentPath) ? Files.readString(contentPath) : "");
                    textsToSave.add(sf);
                } else if ("file".equals(type)) {
                    sf.setFileName(item.getString("fileName", item.getString("name")));
                    sf.setFileSize(item.getLong("fileSize", 0L));
                    if (Files.exists(contentPath)) {
                        filesToUpload.add(Map.entry(sf, contentPath.toFile()));
                    } else {
                        foldersToSave.add(sf);
                    }
                } else {
                    foldersToSave.add(sf);
                }
            }

            // 4. 解析 SKILL.md，生成 skillParameterForm（推断语料 = 所有文本文件内容）
            String skillMd = null;
            StringBuilder corpus = new StringBuilder();
            for (SkillFile sf : textsToSave) {
                if (sf.getContent() != null) corpus.append(sf.getContent()).append('\n');
                if ("SKILL.md".equalsIgnoreCase(sf.getName())) {
                    // 优先取根目录的 SKILL.md
                    if (ROOT_FOLDER_ID.equals(sf.getParentId()) || skillMd == null) {
                        skillMd = sf.getContent();
                    }
                }
            }
            JsonArray paramForm = buildSkillParameterForm(skillMd, corpus.toString());

            // 技能名：内容根目录名（包了一层时）> ZIP 文件名
            String skillName = contentRoot.equals(tempDir)
                    ? upload.fileName().replaceFirst("(?i)\\.zip$", "")
                    : contentRoot.getFileName().toString();
            Skill skill = new Skill(skillId, parentUuId, skillName, "", "", "", new JsonArray(), now, now);
            skill.setSkillParameterForm(paramForm);   // 导入即带表单结构；parameterValue 留空，用户后续设置

            // 5. 创建 skill 资源 + 保存所有 skill_file
            Tool.getNodeRelation(skillRelationMapper, parentUuId, skillId, this::newRelation, this::getAncestorId, this::getDepth)
                    .compose(skillRelationMapper::batch_save)
                    .compose(_ -> skillMapper.save(skill))
                    .compose(_ -> {
                        List<Future<?>> futures = new ArrayList<>();
                        for (SkillFile sf : foldersToSave) futures.add(skillFileMapper.save(sf));
                        for (SkillFile sf : textsToSave) futures.add(skillFileMapper.save(sf));
                        for (var entry : filesToUpload) {
                            SkillFile sf = entry.getKey();
                            java.io.File file = entry.getValue();
                            futures.add(fileMapper.upload(sf.getFileName(), file.length(), null, null, file)
                                    .compose(fe -> {
                                        sf.setFileId(fe.getId());
                                        sf.setFileSize(fe.getSize());
                                        return skillFileMapper.save(sf);
                                    }));
                        }
                        return Future.all(futures);
                    })
                    .onSuccess(_ -> {
                        deleteTempDir(tempDir);
                        context.end(Result.success(skill).toBuffer());
                    })
                    .onFailure(e -> {
                        deleteTempDir(tempDir);
                        context.fail(e);
                    });
        } catch (Exception e) {
            context.fail(e);
        }
    }

    // ============ skillParameterForm 解析 ============

    /**
     * 解析 SKILL.md frontmatter + 脚本文本，生成 skillParameterForm。
     * 优先级：显式 form 声明 > envVars/requires/primaryEnv > 代码推断。
     * type 承载敏感性：PasswordInput 即 secret，不另存 secret 字段；密码字段不写 defaultValue。
     */
    @SuppressWarnings("unchecked")
    public JsonArray buildSkillParameterForm(String skillMd, String scriptCorpus) {
        String body = skillMd == null ? "" : skillMd;
        Map<String, Object> fm = new LinkedHashMap<>();
        if (skillMd != null) {
            Matcher m = FRONTMATTER.matcher(skillMd);
            if (m.find()) {
                try {
                    Object loaded = new Yaml().load(m.group(1));
                    if (loaded instanceof Map<?, ?> mp) fm = (Map<String, Object>) mp;
                } catch (Exception ignored) { /* frontmatter 解析失败则当作无声明 */ }
                body = m.group(2);
            }
        }

        Map<String, Object> meta = fm.get("metadata") instanceof Map<?, ?> mm
                ? (Map<String, Object>) mm : Map.of();
        Map<String, Object> ns = Map.of();
        for (String key : VENDOR_NS) {
            if (meta.get(key) instanceof Map<?, ?> nn) {
                ns = (Map<String, Object>) nn;
                break;
            }
        }

        // ① 显式 form 声明：直接吃进来（支持 Slider/Select 等富类型），不再叠加推断
        if (ns.get("form") instanceof List<?> fields && !fields.isEmpty()) {
            JsonArray form = new JsonArray();
            for (Object o : fields) {
                if (o instanceof Map<?, ?> fld) form.add(normalizeFormField((Map<String, Object>) fld));
            }
            if (!form.isEmpty()) return form;
        }

        String primaryEnv = ns.get("primaryEnv") instanceof String s ? s : null;

        // 先把声明合并（先聚 required/desc，再统一建字段，保证 type 判定能用上 desc）
        LinkedHashMap<String, JsonObject> declared = new LinkedHashMap<>();
        // requires.env → 必填
        if (ns.get("requires") instanceof Map<?, ?> req
                && ((Map<String, Object>) req).get("env") instanceof List<?> envList) {
            for (Object e : envList) mergeDecl(declared, String.valueOf(e), true, null);
        }
        // envVars → 逐变量元数据
        if (ns.get("envVars") instanceof List<?> evs) {
            for (Object o : evs) {
                if (o instanceof String es) mergeDecl(declared, es, false, null);
                else if (o instanceof Map<?, ?> mv) {
                    Map<String, Object> v = (Map<String, Object>) mv;
                    mergeDecl(declared, String.valueOf(v.get("name")),
                            Boolean.TRUE.equals(v.get("required")),
                            v.get("description") == null ? null : String.valueOf(v.get("description")));
                }
            }
        }
        // primaryEnv → 必填
        if (primaryEnv != null) mergeDecl(declared, primaryEnv, true, null);

        // 建字段
        LinkedHashMap<String, JsonObject> form = new LinkedHashMap<>();
        for (var e : declared.entrySet()) {
            JsonObject d = e.getValue();
            addField(form, e.getKey(), d.getBoolean("required", false),
                    d.getString("desc"), false, primaryEnv);
        }

        // ② 代码推断（兜底，标记 inferred、不强制必填）
        for (String name : inferEnvNames(body + "\n" + (scriptCorpus == null ? "" : scriptCorpus))) {
            addField(form, name, false, null, true, primaryEnv);
        }

        JsonArray out = new JsonArray();
        form.values().forEach(out::add);
        return out;
    }

    private void mergeDecl(LinkedHashMap<String, JsonObject> declared, String name,
                           boolean required, String desc) {
        if (name == null || name.isBlank() || "null".equals(name)) return;
        JsonObject d = declared.computeIfAbsent(name,
                k -> new JsonObject().put("required", false));
        if (required) d.put("required", true);
        if (desc != null && !desc.isEmpty()) d.put("desc", desc);
    }

    private void addField(LinkedHashMap<String, JsonObject> form, String name,
                          boolean required, String desc, boolean inferred, String primaryEnv) {
        if (name == null || name.isBlank() || "null".equals(name)) return;
        if (form.containsKey(name)) {
            if (required) form.get(name).put("required", true);   // 已有声明优先，推断只补
            return;
        }
        boolean secret = isSecretEnv(name, desc, name.equals(primaryEnv));
        JsonObject f = new JsonObject()
                .put("field", name)                                       // 真实 env 变量名
                .put("type", secret ? "PasswordInput" : "TextInput")      // type 承载敏感性
                .put("label", name)
                .put("required", required);
        if (desc != null && !desc.isEmpty())
            f.put("attrs", new JsonObject().put("description", desc));
        if (inferred)
            f.put("propsInfo", new JsonObject().put("inferred", true));   // UI 给 Text/Password 切换
        // 注意：PasswordInput 字段不写 defaultValue，避免随 skillParameterForm 导出泄露
        form.put(name, f);
    }

    private boolean isSecretEnv(String name, String description, boolean isPrimary) {
        if (isPrimary) return true;
        if (description != null && SECRET_DESC.matcher(description).find()) return true;
        if (name != null && NONSECRET_NAME.matcher(name).matches()) return false;
        return name != null && SECRET_NAME.matcher(name).find();
    }

    private Set<String> inferEnvNames(String text) {
        Set<String> names = new LinkedHashSet<>();
        if (text == null) return names;
        for (Pattern p : ENV_INFER_PATTERNS) {
            Matcher m = p.matcher(text);
            while (m.find()) {
                String n = m.group(1);
                if (n != null && !ENV_NOISE.contains(n)) names.add(n);
            }
        }
        return names;
    }

    @SuppressWarnings("unchecked")
    private JsonObject normalizeFormField(Map<String, Object> raw) {
        JsonObject f = new JsonObject((Map<String, Object>) new LinkedHashMap<>(raw));
        if (f.getString("type") == null) f.put("type", "TextInput");   // 没 type 默认 TextInput
        if (!f.containsKey("required")) f.put("required", false);
        if (f.getString("label") == null) f.put("label", f.getString("field", ""));
        return f;
    }

    // ============ 导入辅助 ============

    private Path resolveContentRoot(Path tempDir) throws IOException {
        try (var stream = Files.list(tempDir)) {
            List<Path> children = stream.toList();
            if (children.size() == 1 && Files.isDirectory(children.get(0))) {
                return children.get(0);
            }
        }
        return tempDir;
    }

    private String buildFilePathFromStructure(JsonArray structure, JsonObject targetItem) {
        Map<String, JsonObject> itemMap = new HashMap<>();
        for (int i = 0; i < structure.size(); i++) {
            JsonObject item = structure.getJsonObject(i);
            itemMap.put(item.getString("id"), item);
        }
        List<String> parts = new ArrayList<>();
        parts.add(targetItem.getString("name"));
        String parentId = targetItem.getString("parentId", "");
        while (parentId != null && !parentId.isEmpty() && itemMap.containsKey(parentId)) {
            JsonObject parent = itemMap.get(parentId);
            parts.add(0, parent.getString("name"));
            parentId = parent.getString("parentId", "");
        }
        return String.join("/", parts);
    }

    private void deleteTempDir(Path dir) {
        try {
            if (Files.exists(dir)) {
                Files.walk(dir)
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (Exception ignored) {
        }
    }

    private void buildStructureFromDir(Path dir, UUID parentId, JsonArray structure, Map<String, UUID> idMap) throws IOException {
        try (var stream = Files.list(dir)) {
            for (Path child : stream.sorted().toList()) {
                String name = child.getFileName().toString();
                UUID id = UUID.randomUUID();
                JsonObject item = new JsonObject()
                        .put("id", id.toString())
                        .put("parentId", parentId.toString())
                        .put("name", name);
                if (Files.isDirectory(child)) {
                    item.put("type", "folder");
                    structure.add(item);
                    buildStructureFromDir(child, id, structure, idMap);
                } else if (isTextFile(name)) {
                    item.put("type", "text");
                    structure.add(item);
                } else {
                    item.put("type", "file");
                    item.put("fileName", name);
                    item.put("fileSize", Files.size(child));
                    structure.add(item);
                }
            }
        }
    }

    // ============ 资源处理器抽象方法 ============

    @Override
    protected SimpleNodePojo resourceToSimpleNodePojo(Skill skill) {
        SimpleNodePojo simpleNodePojo = new SimpleNodePojo();
        CommonUtils.copyProperties(skill, simpleNodePojo);
        simpleNodePojo.setType("skill");
        return simpleNodePojo;
    }

    @Override
    protected SimpleNodePojo folderToSimpleNodePojo(SkillFolder skillFolder) {
        SimpleNodePojo simpleNodePojo = new SimpleNodePojo();
        CommonUtils.copyProperties(skillFolder, simpleNodePojo);
        simpleNodePojo.setType("folder");
        return simpleNodePojo;
    }

    @Override
    public Boolean resourceRead(RoutingContext context) {
        UserProfile userProfile = context.user().get("user");
        PermissionConstants.Permission permission = PermissionConstants.SKILL_READ.getPermission();
        return userProfile.getPermissions().containsKey(permission.toString());
    }

    @Override
    protected SkillRelation newRelation(UUID id, UUID ancestorId, UUID descendantId, Integer dept) {
        return new SkillRelation(id, ancestorId, descendantId, dept);
    }

    @Override
    protected UUID getParentId(Skill resource) {
        return resource.getParentId();
    }

    @Override
    protected void setName(Skill resource, String name) {
        resource.setName(name);
    }

    @Override
    protected UUID getAncestorId(SkillRelation skillRelation) {
        return skillRelation.getAncestorId();
    }

    @Override
    protected Integer getDepth(SkillRelation skillRelation) {
        return skillRelation.getDepth();
    }

    @Override
    protected String getName(Skill resource) {
        return resource.getName();
    }

    @Override
    protected UUID getTarget(SkillPermission permission) {
        return permission.getTarget();
    }

    @Override
    protected String getPermission(SkillPermission permission) {
        return permission.getPermission();
    }

    @Override
    protected String getNamePrefix() {
        return "新建技能";
    }

    @Override
    protected Skill newResource(UUID resourceId, UUID parentUuId, String name, RoutingContext context) {
        return new Skill(resourceId, parentUuId, name, "", "", "", new JsonArray(), LocalDateTime.now(), LocalDateTime.now());
    }

    @Override
    protected SkillPermission newPermission(UUID id, UUID userId, UUID target, String permission) {
        return new SkillPermission(id, userId, target, permission, LocalDateTime.now(), LocalDateTime.now());
    }
}