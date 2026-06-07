package com.run.workflow.nodes.downloadskills.entity;

import lombok.Data;

import java.util.List;

@Data
public class DownloadSkillsNodeData {
    private String location;
    private List<String> reference;
    private String skillIdLocation;
    private List<String> skillIdReference;
    private String skillId;
}
