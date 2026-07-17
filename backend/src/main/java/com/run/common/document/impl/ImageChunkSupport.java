package com.run.common.document.impl;

import com.run.common.document.Chunk;
import com.run.common.document.DocumentMimes;
import com.run.common.document.ImageChunk;
import com.run.common.document.TextChunk;
import com.run.common.document.UploadedFile;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文档内嵌图片落盘为临时文件并包装成 {@link ImageChunk} 的共享逻辑(DOCX / DOC 复用)。
 * 仿 vertx 上传:只留落盘路径,不携带字节;落盘失败降级为占位文本块,不中断整体提取。
 * 临时文件生命周期交给消费端(转存/清理),这里挂 {@code deleteOnExit} 兜底。
 */
final class ImageChunkSupport {

    private ImageChunkSupport() {
    }

    static Chunk land(byte[] data, String fileName) {
        String mime = DocumentMimes.mimeOf(fileName);
        try {
            String ext = DocumentMimes.extensionOf(fileName);
            Path tmp = Files.createTempFile("doc-img-", ext.isEmpty() ? "" : ("." + ext));
            Files.write(tmp, data);
            tmp.toFile().deleteOnExit();
            UploadedFile upload = new UploadedFile(tmp.toString(), fileName, data != null ? data.length : 0, mime);
            return new ImageChunk("图片", upload);
        } catch (Exception e) {
            return TextChunk.placeholder("图片", "[图片提取失败] " + fileName + ": " + e.getMessage());
        }
    }
}
