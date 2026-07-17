package com.run.common.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 文档提取过程中产出的一块内容的基类。仿 {@code com.run.workflow.message.struct.Content} 的层次:
 * 基类持有 {@link #type} 判别与来源标签,具体内容由子类承载。
 * <p>
 * 消费端按 {@link #type} 或 {@code instanceof} 分流:
 * <ul>
 *     <li>{@link TextChunk}:文本;</li>
 *     <li>{@link ImageChunk}:图片,携带落盘的 {@link UploadedFile}。</li>
 * </ul>
 * chunk 按提取顺序到达,如需序号由消费端自行维护。
 */
@Getter
@Setter
@NoArgsConstructor
public class Chunk {
    /**
     * 块类型
     */
    private ChunkType type;
    /**
     * 来源标签,例如 "第 1 页" / "段落" / "docs/readme.md" / "image1.png",可为空
     */
    private String label;
    /**
     * 是否内联拼接:为 true 时消费端应把本块直接接到上一块之后、<b>不加</b>块级分隔(如 \n\n)。
     * 用于表格单元格内的图片等需要与相邻文本拼在同一行的场景。默认 false(独立成块)。
     */
    private boolean inline;

    protected Chunk(ChunkType type, String label) {
        this.type = type;
        this.label = label;
    }
}
