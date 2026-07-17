package com.run.common.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 文档边界块。标识"从这里开始是一个新文档",用于 zip 等一个物理文件需要拆成多个文档的场景。
 * <p>
 * 消费端语义由自己决定:知识库导入把每个边界切成一个新 Document(用 {@link #path} 还原文件夹树);
 * 只需要单条文本流的消费端(如按整包读取)可忽略边界,或当作一个 {@code ## title} 分节。
 * 没有边界时即视为单文档。
 */
@Getter
@Setter
@NoArgsConstructor
public class DocumentBoundary extends Chunk {
    /**
     * 文档名(通常是条目文件名)
     */
    private String title;
    /**
     * 相对目录路径(以 / 分隔,不含文件名,可为空表示根),用于还原文件夹层级
     */
    private String path;

    public DocumentBoundary(String title, String path) {
        super(ChunkType.DOCUMENT, title);
        this.title = title;
        this.path = path;
    }
}
