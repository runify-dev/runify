package com.run.common.document.impl;

import com.run.common.document.Chunk;
import com.run.common.document.DocumentMimes;
import com.run.common.document.DocumentProcessor;
import com.run.common.document.DocumentSource;
import com.run.common.document.TextChunk;
import com.run.common.document.TextSink;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * DOCX(OOXML)提取:按正文元素逐段/逐表产出 markdown。标题段落转 {@code #} 级标题,
 * 表格渲染为 markdown 表格,段内内嵌图片逐张落盘为图片块。
 * 游标为正文元素迭代位置,资源为 {@link XWPFDocument} 与输入流。
 */
public class DocxProcessor implements DocumentProcessor {

    private XWPFDocument document;
    private InputStream in;

    @Override
    public boolean support(DocumentSource source) {
        return DocumentMimes.DOCX.equals(source.resolvedMime()) || "docx".equals(source.extension());
    }

    @Override
    public void extract(DocumentSource source, TextSink sink) {
        Throwable error = null;
        boolean cancelled = false;
        try {
            in = source.openStream();
            document = new XWPFDocument(in);
            outer:
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    String text = paragraph.getText();
                    if (text != null && !text.isBlank()) {
                        if (!sink.onNext(TextChunk.text("段落", applyHeading(paragraph, text)))) {
                            cancelled = true;
                            break;
                        }
                    }
                    // 段落内嵌图片: 逐张落盘后以 IMAGE 块产出(携带 UploadedFile, 不带字节)
                    for (XWPFRun run : paragraph.getRuns()) {
                        for (XWPFPicture picture : run.getEmbeddedPictures()) {
                            XWPFPictureData pd = picture.getPictureData();
                            if (pd == null) continue;
                            if (!sink.onNext(toImageChunk(pd))) {
                                cancelled = true;
                                break outer;
                            }
                        }
                    }
                } else if (element instanceof XWPFTable table) {
                    // 表格流式产出: 文本片段与单元格内图片按位置交替, 图片片段标记 inline 以拼进同一行
                    if (!streamTable(table, sink)) {
                        cancelled = true;
                        break;
                    }
                }
            }
        } catch (Throwable e) {
            error = e;
        }
        if (!cancelled) {
            sink.onComplete(Optional.ofNullable(error));
        }
    }

    /**
     * 把内嵌图片落盘并包装成图片块,复用 {@link ImageChunkSupport}。
     */
    private Chunk toImageChunk(XWPFPictureData pd) {
        return ImageChunkSupport.land(pd.getData(), pd.getFileName());
    }

    /**
     * 流式渲染表格为 markdown。文本累积进缓冲,遇到单元格内图片时先 flush 当前缓冲(文本片),
     * 再把图片作为 inline 图片块产出,使其拼进同一行的单元格内 {@code | ![](url) | ...}。
     * 返回 false 表示消费端取消。
     */
    private boolean streamTable(XWPFTable table, TextSink sink) {
        StringBuilder buf = new StringBuilder();
        boolean[] first = {true};
        List<XWPFTableRow> rows = table.getRows();
        for (int r = 0; r < rows.size(); r++) {
            List<XWPFTableCell> cells = rows.get(r).getTableCells();
            buf.append('|');
            for (XWPFTableCell cell : cells) {
                buf.append(' ');
                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                    for (XWPFRun run : paragraph.getRuns()) {
                        String t = run.text();
                        if (t != null && !t.isEmpty()) buf.append(mdInline(t));
                        for (XWPFPicture picture : run.getEmbeddedPictures()) {
                            XWPFPictureData pd = picture.getPictureData();
                            if (pd == null) continue;
                            if (!flush(buf, sink, first)) return false;
                            Chunk image = toImageChunk(pd);
                            image.setInline(!first[0]);
                            if (!sink.onNext(image)) return false;
                            first[0] = false;
                        }
                    }
                }
                buf.append(" |");
            }
            buf.append('\n');
            if (r == 0) {
                buf.append('|');
                for (int c = 0; c < cells.size(); c++) buf.append(" --- |");
                buf.append('\n');
            }
        }
        return flush(buf, sink, first);
    }

    /**
     * 把当前缓冲作为文本块产出(非空才发):表格首个片段独立成块(非 inline),其后片段 inline 拼接。
     */
    private boolean flush(StringBuilder buf, TextSink sink, boolean[] first) {
        if (buf.length() == 0) return true;
        TextChunk chunk = new TextChunk("表格", buf.toString(), false);
        chunk.setInline(!first[0]);
        first[0] = false;
        buf.setLength(0);
        return sink.onNext(chunk);
    }

    /**
     * 单元格内文本清理:去控制符、换行转空格、转义竖线,保持在同一行内。
     */
    private String mdInline(String s) {
        if (s == null) return "";
        return s.replace('\r', ' ').replace('\n', ' ')
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "")
                .replace("|", "\\|");
    }

    /**
     * 标题样式段落转 markdown {@code #} 级标题(styleId 含 heading/标题 时按其末尾数字定级)。
     */
    private String applyHeading(XWPFParagraph paragraph, String text) {
        String styleId = paragraph.getStyleID();
        if (styleId == null) return text;
        String s = styleId.toLowerCase();
        if (!s.contains("heading") && !styleId.contains("标题")) return text;
        int level = 1;
        for (int i = s.length() - 1; i >= 0; i--) {
            if (Character.isDigit(s.charAt(i))) {
                level = s.charAt(i) - '0';
                break;
            }
        }
        if (level < 1) level = 1;
        if (level > 6) level = 6;
        return "#".repeat(level) + " " + text;
    }

    @Override
    public void close() {
        if (document != null) {
            try {
                document.close();
            } catch (IOException ignored) {
            }
            document = null;
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException ignored) {
            }
            in = null;
        }
    }
}
