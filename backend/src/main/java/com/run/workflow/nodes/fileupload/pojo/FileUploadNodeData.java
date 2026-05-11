package com.run.workflow.nodes.fileupload.pojo;

import lombok.Data;

import java.util.List;

@Data
public class FileUploadNodeData {
    /**
     * 路径来源: reference 或 customize
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
