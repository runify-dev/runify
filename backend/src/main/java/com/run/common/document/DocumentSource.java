package com.run.common.document;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 待提取的文档来源。同时携带磁盘路径、文件名与 mime,便于处理器按需选择读取方式:
 * PDF/DOCX 通常直接用 {@link #path()},压缩包等可用 {@link #openStream()} 顺序读。
 *
 * @param path     文件在磁盘上的路径
 * @param fileName 原始文件名(用于扩展名判断与展示)
 * @param mimeType mime 类型,可为空(为空时由处理器按扩展名推断)
 */
public record DocumentSource(Path path, String fileName, String mimeType) {

    /**
     * 由路径构造,文件名取路径末段,mime 按扩展名推断。
     */
    public static DocumentSource of(Path path) {
        String name = path.getFileName() != null ? path.getFileName().toString() : "";
        return new DocumentSource(path, name, DocumentMimes.mimeOf(name));
    }

    /**
     * 由路径 + 显式文件名构造(上传场景原始文件名可能与落盘名不同)。
     */
    public static DocumentSource of(Path path, String fileName) {
        return new DocumentSource(path, fileName, DocumentMimes.mimeOf(fileName));
    }

    /**
     * 打开一个新的输入流,调用方负责关闭。
     */
    public InputStream openStream() throws IOException {
        return Files.newInputStream(path);
    }

    /**
     * 有效 mime:显式值优先,缺省按扩展名推断。
     */
    public String resolvedMime() {
        return mimeType != null ? mimeType : DocumentMimes.mimeOf(fileName);
    }

    /**
     * 扩展名(小写,不含点),无扩展名返回空串。
     */
    public String extension() {
        return DocumentMimes.extensionOf(fileName);
    }
}
