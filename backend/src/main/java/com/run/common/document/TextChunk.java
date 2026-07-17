package com.run.common.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 文本块。仿 {@code TextContent extends Content}。
 */
@Getter
@Setter
@NoArgsConstructor
public class TextChunk extends Chunk {
    /**
     * 文本内容
     */
    private String content;
    /**
     * 是否为占位文本块(如压缩包内的二进制条目、截断说明,content 只是说明而非真实正文,消费端可选择跳过)
     */
    private boolean placeholder;

    public TextChunk(String label, String content, boolean placeholder) {
        super(ChunkType.TEXT, label);
        this.content = content;
        this.placeholder = placeholder;
    }

    public static TextChunk text(String label, String content) {
        return new TextChunk(label, content, false);
    }

    public static TextChunk placeholder(String label, String content) {
        return new TextChunk(label, content, true);
    }

    @Override
    public String toString() {
        return content;
    }
}
