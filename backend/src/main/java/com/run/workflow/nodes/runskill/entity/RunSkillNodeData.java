package com.run.workflow.nodes.runskill.entity;

import lombok.Data;

import java.util.List;

@Data
public class RunSkillNodeData {
    private String location;
    private List<String> reference;
    private String skillIdLocation;
    private List<String> skillIdReference;
    private String skillId;
    private String commandLocation;
    private List<String> commandReference;
    private String command;
    private String runtime;
}
