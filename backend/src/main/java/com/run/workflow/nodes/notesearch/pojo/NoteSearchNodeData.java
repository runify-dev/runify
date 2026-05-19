package com.run.workflow.nodes.notesearch.pojo;

import lombok.Data;

import java.util.List;

@Data
public class NoteSearchNodeData {
    /**
     * 目录ID列表（可选，为空则不限目录）
     */
    private List<String> folderIds;
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
