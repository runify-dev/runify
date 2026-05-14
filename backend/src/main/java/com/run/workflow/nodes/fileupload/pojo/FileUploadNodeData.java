package com.run.workflow.nodes.fileupload.pojo;

import lombok.Data;

import java.util.List;

@Data
public class FileUploadNodeData {
    /**
     * 顶层模式: tool_call 或 customize
     */
    private String location;
    /**
     * tool_call 引用变量路径
     */
    private List<String> reference;
    /**
     * 路径子模式: reference 或 customize
     */
    private String pathLocation;
    /**
     * 路径引用变量路径
     */
    private List<String> pathReference;
    /**
     * 自定义路径
     */
    private String path;
    /**
     * 自定义文件名（可选）
     */
    private String fileName;
}
