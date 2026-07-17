package com.run.common.document.impl;

import com.run.common.document.Chunk;
import com.run.common.document.DocumentBoundary;
import com.run.common.document.DocumentMimes;
import com.run.common.document.DocumentProcessor;
import com.run.common.document.DocumentProcessorRegistry;
import com.run.common.document.DocumentSource;
import com.run.common.document.TextChunk;
import com.run.common.document.TextSink;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * ZIP 组合式提取:把压缩包拆成多个文档。逐条目 →
 * <ul>
 *     <li>产出 {@link DocumentBoundary}(标题=条目名,路径=条目所在目录) 开启一个新文档;</li>
 *     <li>markdown 条目:解析其中相对路径的图片引用 {@code ![](./x.png)},在同包内找到对应图片则
 *     入库并原位替换为 inline 图片块,外链/找不到的保持原样;</li>
 *     <li>纯文本条目:整段文本;</li>
 *     <li>pdf/docx/doc 条目:委托对应处理器提取(转发其 chunk);</li>
 *     <li>嵌套 zip:递归展开(带深度上限),以其文件名作为子目录;</li>
 *     <li>单独的图片条目:跳过(被 markdown 引用的图片在解析引用时按需读取);</li>
 *     <li>其它二进制:跳过。</li>
 * </ul>
 * 防解压炸弹:限制条目数、单条目字节、累计字节与嵌套深度。
 */
public class ZipProcessor implements DocumentProcessor {

    private static final int MAX_ENTRIES = 5000;
    private static final long MAX_ENTRY_BYTES = 20L * 1024 * 1024;
    private static final long MAX_TOTAL_BYTES = 200L * 1024 * 1024;
    private static final int MAX_ZIP_DEPTH = 3;

    /**
     * markdown 图片语法 {@code ![alt](url "可选标题")}
     */
    private static final Pattern MD_IMAGE = Pattern.compile("!\\[([^\\]]*)\\]\\(\\s*([^)\\s]+)(?:\\s+\"[^\"]*\")?\\s*\\)");

    private ZipFile zipFile;
    private long totalBytes = 0;
    private final boolean[] cancelled = {false};

    @Override
    public boolean support(DocumentSource source) {
        return DocumentMimes.ZIP.equals(source.resolvedMime()) || "zip".equals(source.extension());
    }

    @Override
    public void extract(DocumentSource source, TextSink sink) {
        Throwable error = null;
        try {
            zipFile = new ZipFile(source.path().toFile());
            processZip(zipFile, "", 0, sink);
        } catch (Throwable e) {
            error = e;
        }
        if (!cancelled[0]) {
            sink.onComplete(Optional.ofNullable(error));
        }
    }

    private void processZip(ZipFile zip, String prefix, int depth, TextSink sink) throws Exception {
        Enumeration<? extends ZipEntry> entries = zip.entries();
        int count = 0;
        while (entries.hasMoreElements() && !cancelled[0]) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) continue;
            if (++count > MAX_ENTRIES || totalBytes >= MAX_TOTAL_BYTES) break;

            String name = entry.getName();
            String fileName = baseName(name);
            String dir = joinPath(prefix, dirOf(name));
            String ext = DocumentMimes.extensionOf(fileName);
            String mime = DocumentMimes.mimeOf(fileName);

            if (mime.startsWith("image/")) {
                // 单独图片: 跳过(被 markdown 引用的图在解析引用时按需读取)
                continue;
            }
            if (DocumentMimes.ZIP.equals(mime)) {
                if (depth + 1 > MAX_ZIP_DEPTH) continue;
                Path tmp = writeTemp(zip, entry, ".zip");
                if (tmp == null) continue;
                try (ZipFile inner = new ZipFile(tmp.toFile())) {
                    processZip(inner, joinPath(dir, stripExt(fileName)), depth + 1, sink);
                } catch (Exception ignored) {
                    // 坏的嵌套 zip: 跳过
                } finally {
                    Files.deleteIfExists(tmp);
                }
                continue;
            }
            if (DocumentMimes.isTextual(mime)) {
                if (!emit(sink, new DocumentBoundary(fileName, dir))) return;
                String text = new String(readCapped(zip, entry), StandardCharsets.UTF_8);
                if ("md".equals(ext) || "markdown".equals(ext)) {
                    if (!streamMarkdown(text, dir, zip, sink)) return;
                } else {
                    if (!emit(sink, TextChunk.text(fileName, text))) return;
                }
                continue;
            }
            // pdf/docx/doc → 委托对应处理器
            Path tmp = writeTemp(zip, entry, ext.isEmpty() ? "" : ("." + ext));
            if (tmp == null) continue;
            try {
                DocumentSource subSource = DocumentSource.of(tmp, fileName);
                DocumentProcessor processor = DocumentProcessorRegistry.getInstance().pick(subSource);
                if (processor == null || processor instanceof ZipProcessor) {
                    continue; // 其它二进制: 跳过
                }
                if (!emit(sink, new DocumentBoundary(fileName, dir))) return;
                ForwardingSink forwarding = new ForwardingSink(sink);
                try (processor) {
                    processor.extract(subSource, forwarding);
                } catch (Exception ignored) {
                    // 子文档解析失败: 保留已产出的部分, 继续下一个条目
                }
                if (forwarding.cancelled) {
                    cancelled[0] = true;
                    return;
                }
            } finally {
                Files.deleteIfExists(tmp);
            }
        }
    }

    /**
     * 流式渲染 markdown 并解析相对图片引用:命中同包内图片则原位替换为 inline 图片块。
     */
    private boolean streamMarkdown(String md, String baseDir, ZipFile zip, TextSink sink) {
        Matcher m = MD_IMAGE.matcher(md);
        StringBuilder buf = new StringBuilder();
        boolean[] first = {true};
        int last = 0;
        while (m.find()) {
            String refKey = resolveImageRef(baseDir, m.group(2), zip);
            if (refKey == null) continue; // 外链/找不到/非图片: 保留原文, 不切分
            buf.append(md, last, m.start());
            if (!flush(buf, sink, first)) return false;
            Chunk image = landRef(zip, refKey);
            if (image != null) {
                image.setInline(!first[0]);
                if (!sink.onNext(image)) {
                    cancelled[0] = true;
                    return false;
                }
                first[0] = false;
            } else {
                // 读取失败: 保留原始引用文本
                buf.append(md, m.start(), m.end());
            }
            last = m.end();
        }
        buf.append(md.substring(last));
        return flush(buf, sink, first);
    }

    private boolean flush(StringBuilder buf, TextSink sink, boolean[] first) {
        if (buf.length() == 0) return true;
        TextChunk chunk = new TextChunk("markdown", buf.toString(), false);
        chunk.setInline(!first[0]);
        first[0] = false;
        buf.setLength(0);
        if (!sink.onNext(chunk)) {
            cancelled[0] = true;
            return false;
        }
        return true;
    }

    /**
     * 解析 markdown 图片引用的相对路径,命中同包内图片条目则返回其 zip 内路径,否则 null。
     */
    private String resolveImageRef(String baseDir, String url, ZipFile zip) {
        if (url == null || url.isEmpty()) return null;
        String u = url.trim();
        if (u.matches("(?i)^(https?:|data:|//|mailto:).*")) return null; // 外链
        int hash = u.indexOf('#');
        if (hash >= 0) u = u.substring(0, hash);
        int q = u.indexOf('?');
        if (q >= 0) u = u.substring(0, q);
        try {
            u = URLDecoder.decode(u, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
        String combined = joinPath(baseDir, u);
        String key = normalize(combined);
        if (key.isEmpty()) return null;
        ZipEntry entry = zip.getEntry(key);
        if (entry == null || entry.isDirectory()) return null;
        if (!DocumentMimes.mimeOf(key).startsWith("image/")) return null;
        return key;
    }

    private Chunk landRef(ZipFile zip, String key) {
        try {
            ZipEntry entry = zip.getEntry(key);
            byte[] bytes = readCapped(zip, entry);
            return ImageChunkSupport.land(bytes, baseName(key));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean emit(TextSink sink, Chunk chunk) {
        if (!sink.onNext(chunk)) {
            cancelled[0] = true;
            return false;
        }
        return true;
    }

    /**
     * 读取条目内容,最多 {@link #MAX_ENTRY_BYTES},并计入总量预算。
     */
    private byte[] readCapped(ZipFile zip, ZipEntry entry) throws IOException {
        long budget = Math.min(MAX_ENTRY_BYTES, MAX_TOTAL_BYTES - totalBytes);
        try (InputStream in = zip.getInputStream(entry)) {
            byte[] bytes = in.readNBytes((int) Math.max(0, budget));
            totalBytes += bytes.length;
            return bytes;
        }
    }

    /**
     * 把条目写到临时文件供子处理器读取。超出单条目上限或总量时返回 null。
     */
    private Path writeTemp(ZipFile zip, ZipEntry entry, String suffix) throws IOException {
        byte[] bytes = readCapped(zip, entry);
        if (bytes.length == 0) return null;
        Path tmp = Files.createTempFile("zip-entry-", suffix);
        Files.write(tmp, bytes);
        tmp.toFile().deleteOnExit();
        return tmp;
    }

    // ---- 路径工具(zip 内部一律用 / 分隔) ----

    private String baseName(String path) {
        String p = path.replace('\\', '/');
        int i = p.lastIndexOf('/');
        return i >= 0 ? p.substring(i + 1) : p;
    }

    private String dirOf(String path) {
        String p = path.replace('\\', '/');
        int i = p.lastIndexOf('/');
        return i >= 0 ? p.substring(0, i) : "";
    }

    private String stripExt(String fileName) {
        int i = fileName.lastIndexOf('.');
        return i > 0 ? fileName.substring(0, i) : fileName;
    }

    private String joinPath(String a, String b) {
        if (a == null || a.isEmpty()) return b == null ? "" : b;
        if (b == null || b.isEmpty()) return a;
        return a + "/" + b;
    }

    /**
     * 规范化 zip 内相对路径:处理 ./ 与 ../,统一 /,去掉前导 ./ 与 /。
     */
    private String normalize(String path) {
        String[] parts = path.replace('\\', '/').split("/");
        java.util.Deque<String> stack = new java.util.ArrayDeque<>();
        for (String part : parts) {
            if (part.isEmpty() || part.equals(".")) continue;
            if (part.equals("..")) {
                if (!stack.isEmpty()) stack.removeLast();
            } else {
                stack.addLast(part);
            }
        }
        return String.join("/", stack);
    }

    @Override
    public void close() {
        if (zipFile != null) {
            try {
                zipFile.close();
            } catch (IOException ignored) {
            }
            zipFile = null;
        }
    }

    /**
     * 转发子处理器的 chunk 给上层 sink;吞掉子处理器的 onComplete(仅记录取消/错误)。
     */
    private static final class ForwardingSink implements TextSink {
        private final TextSink target;
        boolean cancelled = false;
        Throwable error;

        ForwardingSink(TextSink target) {
            this.target = target;
        }

        @Override
        public boolean onNext(Chunk chunk) {
            boolean cont = target.onNext(chunk);
            if (!cont) cancelled = true;
            return cont;
        }

        @Override
        public void onComplete(Optional<Throwable> err) {
            err.ifPresent(e -> error = e);
        }
    }
}
