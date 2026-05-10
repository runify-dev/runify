package com.run.workflow.nodes.databaseinsert.pojo;

import com.run.workflow.nodes.databasesearch.pojo.DatabaseSearchNodeData;
import lombok.Data;

import java.util.List;

@Data
public class DatabaseInsertNodeData {
    private String poolId;
    /**
     * reference / customize
     */
    private String location;
    private List<String> reference;
    private String template;
    private List<DatabaseSearchNodeData.Parameter> parameters;
}
