package com.run.common.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 图片块。携带落盘的 {@link UploadedFile}(而非字节),消费端可挂多模态 / 转存 / 忽略。
 * 仿 {@code Content} 的子类风格。
 */
@Getter
@Setter
@NoArgsConstructor
public class ImageChunk extends Chunk {
    /**
     * 落盘的文件对象
     */
    private UploadedFile upload;

    public ImageChunk(String label, UploadedFile upload) {
        super(ChunkType.IMAGE, label);
        this.upload = upload;
    }
}
