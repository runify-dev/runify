package com.run.common.document;

/**
 * 文档提取块类型判别。仿 {@code ContentTypeConstants} 的用法。
 */
public enum ChunkType {
    /**
     * 文本块
     */
    TEXT,
    /**
     * 图片块(携带落盘的文件上传对象)
     */
    IMAGE,
    /**
     * 文档边界:标识后续 chunk 属于一个新文档(用于 zip 等一个文件拆多文档的场景)
     */
    DOCUMENT
}
