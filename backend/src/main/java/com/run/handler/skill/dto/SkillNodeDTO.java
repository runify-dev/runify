package com.run.handler.skill.dto;

import com.run.handler.common.pojo.SimpleNodePojo;
import com.run.workflow.nodes.response.pojo.ResponseNodeData;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkillNodeDTO extends SimpleNodePojo {
    private JsonObject meta;
}
