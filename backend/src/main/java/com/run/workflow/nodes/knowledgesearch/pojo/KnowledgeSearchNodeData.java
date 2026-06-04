package com.run.workflow.nodes.knowledgesearch.pojo;

import lombok.Data;

import java.util.List;

@Data
public class KnowledgeSearchNodeData {
    /**
     * 知识库ID列表
     */
    private List<String> knowledgeIds;
    /**
     * 顶层模式: tool_call 或 customize
     */
    private String location;
    /**
     * tool_call 引用变量路径
     */
    private List<String> reference;
    private String keywordLocation;
    private List<String> keywordReference;
    private String keyword;
    private String pageNoLocation;
    private List<String> pageNoReference;
    private Integer pageNo;
    private String pageSizeLocation;
    private List<String> pageSizeReference;
    private Integer pageSize;
}
