package com.run.workflow.entity;

import com.run.workflow.NodeStatus;
import lombok.Data;

@Data
public class NodeInfo {
    private String id;
    private String name;
    private String type;
    private NodeStatus status;
}
