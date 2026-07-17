package com.run.common.document;

/**
 * 文件名 → mime 的轻量推断,以及"是否文本类型"的判断。供文档处理器共用。
 */
public final class DocumentMimes {

    public static final String PDF = "application/pdf";
    public static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    public static final String DOC = "application/msword";
    public static final String ZIP = "application/zip";

    private DocumentMimes() {
    }

    /**
     * 扩展名(小写,不含点)。无扩展名返回空串。
     */
    public static String extensionOf(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return "";
        return fileName.substring(dot + 1).toLowerCase();
    }

    public static String mimeOf(String fileName) {
        return switch (extensionOf(fileName)) {
            case "pdf" -> PDF;
            case "docx" -> DOCX;
            case "doc" -> DOC;
            case "zip" -> ZIP;
            case "gz" -> "application/gzip";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "mov" -> "video/quicktime";
            case "avi" -> "video/x-msvideo";
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "ogg" -> "audio/ogg";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "csv" -> "text/csv";
            case "txt", "log" -> "text/plain";
            case "html", "htm" -> "text/html";
            case "css" -> "text/css";
            case "js", "mjs" -> "text/javascript";
            case "ts" -> "text/typescript";
            case "md", "markdown" -> "text/markdown";
            case "java" -> "text/x-java";
            case "py" -> "text/x-python";
            case "sh" -> "text/x-shellscript";
            case "yaml", "yml" -> "text/yaml";
            case "toml" -> "text/toml";
            case "sql" -> "text/x-sql";
            case "properties", "ini", "conf", "cfg" -> "text/plain";
            default -> "application/octet-stream";
        };
    }

    /**
     * 是否文本类型(可直接按 UTF-8 读取)。
     */
    public static boolean isTextual(String mime) {
        if (mime == null) return false;
        return mime.startsWith("text/")
                || mime.equals("application/json")
                || mime.equals("application/xml")
                || mime.equals("application/javascript");
    }

    /**
     * 按文件名判断是否文本类型。
     */
    public static boolean isTextualFile(String fileName) {
        return isTextual(mimeOf(fileName));
    }
}
