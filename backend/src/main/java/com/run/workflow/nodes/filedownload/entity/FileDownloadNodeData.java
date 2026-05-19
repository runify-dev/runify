package com.run.workflow.nodes.filedownload.entity;

import lombok.Data;

import java.util.List;

@Data
public class FileDownloadNodeData {
    /**
     * 顶层模式: tool_call 或 customize
     */
    private String location;
    /**
     * tool_call 引用变量路径
     */
    private List<String> reference;
    /**
     * 文件ID 子模式: reference 或 customize
     */
    private String fileIdLocation;
    /**
     * 文件ID 引用变量路径
     */
    private List<String> fileIdReference;
    /**
     * 自定义文件ID
     */
    private String fileId;
    /**
     * 输出路径 子模式: reference 或 customize
     */
    private String pathLocation;
    /**
     * 输出路径 引用变量路径
     */
    private List<String> pathReference;
    /**
     * 自定义输出路径
     */
    private String path;
}
