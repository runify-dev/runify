package com.run.workflow.nodes.listskills;

import com.run.RunApplication;
import com.run.common.util.CommonUtils;
import com.run.dao.entity.Skill;
import com.run.dao.mapper.SkillMapper;
import com.run.sql.DSL;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.ToolCallContent;
import com.run.workflow.nodes.listskills.entity.ListSkillsNodeData;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class ListSkillsNode extends INode<ListSkillsNode, ListSkillsNodeData> {

    public final static String type = "list-skills-node";
    public final static List<WorkflowType> supportWorkflow = List.of(
            WorkflowType.CHAT_WORKFLOW,
            WorkflowType.CHAT_WORKFLOW_LOOP,
            WorkflowType.PROCESSOR_HTTP,
            WorkflowType.PROCESSOR_HTTP_LOOP
    );

    public ListSkillsNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public ListSkillsNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    record ListSkillsConfig(String chunkId, boolean withWriteArguments, ToolCallMeta meta) {}

    public static class Handle implements BiFunction<WorkFlowManage, ListSkillsNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, ListSkillsNode node) {
            String runId = (String) workFlowManage.getParams().get("workflowRunId");
            ListSkillsConfig config = resolveConfig(node, workFlowManage);

            if (config.withWriteArguments()) {
                workFlowManage.write(node, new ToolCallContent("list_skills", "",
                        "{}", NodeStatus.RUNNING, node, runId, config.chunkId()));
            }

            try {
                SkillMapper skillMapper = RunApplication.appComponent.skillMapper();
                List<Skill> skills = skillMapper.list(DSL.noCondition())
                        .toCompletionStage().toCompletableFuture().join();

                JsonArray skillsArray = new JsonArray();
                List<String> lines = new ArrayList<>();
                Path skillsDir = workFlowManage.getApplicationDirectory().resolve("skills");

                for (Skill skill : skills) {
                    String name = skill.getName() != null ? skill.getName() : "";
                    String desc = skill.getDesc() != null ? skill.getDesc() : "";
                    String id = skill.getId().toString();

                    // 检查是否已安装以及是否有更新
                    Path skillDir = skillsDir.resolve(name);
                    Path metaFile = skillDir.resolve(".skill-meta.json");
                    boolean installed = Files.exists(metaFile);
                    boolean hasUpdate = false;

                    if (installed && skill.getUpdateTime() != null) {
                        try {
                            String metaJson = Files.readString(metaFile);
                            JsonObject meta = new JsonObject(metaJson);
                            String localUpdateTime = meta.getString("skillUpdateTime");
                            if (localUpdateTime != null) {
                                java.time.LocalDateTime localTime = java.time.LocalDateTime.parse(localUpdateTime);
                                hasUpdate = skill.getUpdateTime().isAfter(localTime);
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    String status = hasUpdate ? "[有更新]" : installed ? "[已安装]" : "[未安装]";

                    JsonObject skillObj = new JsonObject();
                    skillObj.put("id", id);
                    skillObj.put("name", name);
                    skillObj.put("description", desc);
                    skillObj.put("installed", installed);
                    skillObj.put("hasUpdate", hasUpdate);
                    if (installed) skillObj.put("local", skillDir.toString());
                    if (skill.getSkillParameterForm() != null) {
                        skillObj.put("parameters", skill.getSkillParameterForm());
                    }
                    skillsArray.add(skillObj);

                    // 格式化输出
                    StringBuilder sb = new StringBuilder();
                    sb.append(status).append(" ").append(name).append("\n");
                    sb.append("  id: ").append(id).append("\n");
                    sb.append("  description: ").append(desc).append("\n");
                    if (installed) {
                        sb.append("  local: ").append(skillDir).append("\n");
                    }
                    String block = sb.toString();
                    lines.add(block);
                    workFlowManage.write(node, new ToolCallContent("list_skills", block, "",
                            NodeStatus.RUNNING, node, runId, config.chunkId()));
                }

                String content = String.join("\n", lines).stripTrailing();
                long installedCount = skillsArray.stream()
                        .filter(o -> ((JsonObject) o).getBoolean("installed"))
                        .count();
                long updateCount = skillsArray.stream()
                        .filter(o -> ((JsonObject) o).getBoolean("hasUpdate"))
                        .count();
                String summary = installedCount + " 已安装" + (updateCount > 0 ? ", " + updateCount + " 有更新" : "") + ", " + (skills.size() - installedCount) + " 未安装";
                String argsJson = "{}";

                workFlowManage.writeContext(node, "content", content);
                workFlowManage.writeContext(node, "skills", skillsArray);
                workFlowManage.writeContext(node, "summary", summary);
                workFlowManage.writeContext(node, "tool", JsonObject.mapFrom(
                        new ToolCallContent("list_skills", content, argsJson,
                                NodeStatus.SUCCESS, node, runId, config.chunkId()).withMeta(config.meta())));
                node.status = NodeStatus.SUCCESS;

                workFlowManage.write(node, new ToolCallContent("list_skills", "", "",
                        NodeStatus.SUCCESS, node, runId, config.chunkId()));

            } catch (Exception e) {
                node.status = NodeStatus.FAIL;
                workFlowManage.writeContext(node, "tool", JsonObject.mapFrom(
                        new ToolCallContent("list_skills", e.getMessage(), "",
                                NodeStatus.FAIL, node, runId, config.chunkId()).withMeta(config.meta())));
                workFlowManage.nextFailInvoke(node, e);
                return null;
            }

            return workFlowManage.nextNodeSupplier(node.node.getId());
        }

        private ListSkillsConfig resolveConfig(ListSkillsNode node, WorkFlowManage wfm) {
            String chunkId = CommonUtils.uuid7().toString();
            ToolCallMeta meta = ToolCallMeta.EMPTY;

            if ("tool_call".equals(node.params.getLocation())) {
                if (node.params.getReference() != null && !node.params.getReference().isEmpty()) {
                    Object ref = wfm.getContextVariable(node.params.getReference());
                    if (ref instanceof JsonObject jo) {
                        meta = ToolCallMeta.from(jo);
                        String id = jo.getString("id");
                        if (!StringUtils.isEmpty(id)) chunkId = id;
                    }
                }
                return new ListSkillsConfig(chunkId, false, meta);
            }

            return new ListSkillsConfig(chunkId, true, ToolCallMeta.EMPTY);
        }
    }

    @Override
    public ListSkillsNodeData getNodeData(JsonObject params) {
        JsonObject jsonObject = node.getProperties().getJsonObject("nodeData");
        ListSkillsNodeData data = new ListSkillsNodeData();
        data.setLocation(jsonObject.getString("location"));
        if (jsonObject.getJsonArray("reference") != null) {
            data.setReference(jsonObject.getJsonArray("reference").stream().map(Object::toString).toList());
        }
        return data;
    }

    @Override
    public NodeResult<ListSkillsNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
