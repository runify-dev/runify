package com.run.common.document.impl;

import com.run.common.document.Chunk;
import com.run.common.document.DocumentMimes;
import com.run.common.document.DocumentProcessor;
import com.run.common.document.DocumentSource;
import com.run.common.document.TextChunk;
import com.run.common.document.TextSink;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.model.PicturesTable;
import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Picture;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableRow;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * DOC(老版二进制 Word,HWPF)提取:逐段产出文本,段内字符游程中的内嵌图片逐张落盘为图片块。
 * 资源为 {@link HWPFDocument} 与输入流。
 */
public class DocProcessor implements DocumentProcessor {

    private HWPFDocument document;
    private InputStream in;

    @Override
    public boolean support(DocumentSource source) {
        return DocumentMimes.DOC.equals(source.resolvedMime()) || "doc".equals(source.extension());
    }

    @Override
    public void extract(DocumentSource source, TextSink sink) {
        Throwable error = null;
        boolean cancelled = false;
        try {
            in = source.openStream();
            document = new HWPFDocument(in);
            PicturesTable pictures = document.getPicturesTable();
            Range range = document.getRange();
            int paragraphs = range.numParagraphs();
            outer:
            for (int i = 0; i < paragraphs; ) {
                Paragraph paragraph = range.getParagraph(i);
                if (paragraph.isInTable()) {
                    // 表格流式产出: 文本片段与单元格内图片按位置交替(图片 inline 拼进同一行), 并跳过表格内段落
                    Table table = range.getTable(paragraph);
                    if (!streamTable(table, pictures, sink)) {
                        cancelled = true;
                        break;
                    }
                    int end = table.getEndOffset();
                    i++;
                    while (i < paragraphs && range.getParagraph(i).getStartOffset() < end) {
                        i++;
                    }
                    continue;
                }
                String text = clean(paragraph.text());
                if (!text.isBlank()) {
                    if (!sink.onNext(TextChunk.text("段落", text))) {
                        cancelled = true;
                        break;
                    }
                }
                // 段内内嵌图片
                for (int r = 0; r < paragraph.numCharacterRuns(); r++) {
                    CharacterRun run = paragraph.getCharacterRun(r);
                    if (pictures.hasPicture(run)) {
                        Picture picture = pictures.extractPicture(run, false);
                        if (!sink.onNext(ImageChunkSupport.land(picture.getContent(), picture.suggestFullFileName()))) {
                            cancelled = true;
                            break outer;
                        }
                    }
                }
                i++;
            }
        } catch (Throwable e) {
            error = e;
        }
        if (!cancelled) {
            sink.onComplete(Optional.ofNullable(error));
        }
    }

    /**
     * 流式渲染表格为 markdown。文本累积进缓冲,遇到单元格内图片时先 flush 当前缓冲(文本片),
     * 再把图片作为 inline 图片块产出,使其拼进同一行的单元格内 {@code | ![](url) | ...}。
     * 返回 false 表示消费端取消。
     */
    private boolean streamTable(Table table, PicturesTable pictures, TextSink sink) {
        StringBuilder buf = new StringBuilder();
        boolean[] first = {true};
        int rows = table.numRows();
        for (int r = 0; r < rows; r++) {
            TableRow row = table.getRow(r);
            int cells = row.numCells();
            buf.append('|');
            for (int c = 0; c < cells; c++) {
                TableCell cell = row.getCell(c);
                buf.append(' ');
                for (int cr = 0; cr < cell.numCharacterRuns(); cr++) {
                    CharacterRun run = cell.getCharacterRun(cr);
                    if (pictures.hasPicture(run)) {
                        if (!flush(buf, sink, first)) return false;
                        Picture picture = pictures.extractPicture(run, false);
                        Chunk image = ImageChunkSupport.land(picture.getContent(), picture.suggestFullFileName());
                        image.setInline(!first[0]);
                        if (!sink.onNext(image)) return false;
                        first[0] = false;
                    } else {
                        buf.append(mdInline(run.text()));
                    }
                }
                buf.append(" |");
            }
            buf.append('\n');
            if (r == 0) {
                buf.append('|');
                for (int c = 0; c < cells; c++) buf.append(" --- |");
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
     * HWPF 段落文本以 \r 结尾,并可能夹杂控制符(表格单元格标记、图片锚点等),统一清理。
     */
    private String clean(String text) {
        if (text == null) return "";
        return text.replace('\r', '\n')
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "")
                .stripTrailing();
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
