package com.run.workflow.nodes.listskills.entity;

import lombok.Data;

import java.util.List;

@Data
public class ListSkillsNodeData {
    private String location;
    private List<String> reference;
}
