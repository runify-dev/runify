package com.run.common.document;

import com.run.common.pojo.File;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 仿 Vertx {@code io.vertx.ext.web.FileUpload} 的已落盘文件对象:提取出的二进制内容(如图片)先落盘,
 * 这里只持有落盘路径与元信息,不携带字节。消费端可据此挂多模态 / 转存 / 忽略并清理。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadedFile {
    /**
     * 已落盘的临时文件路径(对应 vertx {@code uploadedFileName()})
     */
    private String uploadedFileName;
    /**
     * 原始文件名(对应 vertx {@code fileName()})
     */
    private String fileName;
    /**
     * 字节数
     */
    private long size;
    /**
     * mime 类型
     */
    private String contentType;

    /**
     * 转成消息层使用的轻量文件对象(url 指向落盘路径)。
     */
    public File toFile() {
        File file = new File();
        file.setUrl(uploadedFileName);
        file.setName(fileName);
        return file;
    }
}
