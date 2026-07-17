package com.run.common.document.impl;

import com.run.common.document.Chunk;
import com.run.common.document.DocumentMimes;
import com.run.common.document.DocumentProcessor;
import com.run.common.document.DocumentSource;
import com.run.common.document.TextChunk;
import com.run.common.document.TextSink;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * PDF 提取:逐页产出文本(markdown),并逐页抽取内嵌图片为图片块。
 * 图片尽量保留原始格式(JPEG 走原始 DCT 流精确保留,其余无损 PNG 兜底);同一图片对象跨页去重。
 * 游标为当前页号,资源为 {@link PDDocument}。
 */
public class PdfProcessor implements DocumentProcessor {

    /**
     * 取原始 JPEG 字节时,保留 DCTDecode 滤镜(其解码前的流本身就是一张 jpg)。
     */
    private static final List<String> DCT_FILTERS = List.of(
            COSName.DCT_DECODE.getName(), COSName.DCT_DECODE_ABBREVIATION.getName());

    private static final int MAX_FORM_DEPTH = 8;

    private PDDocument document;
    /**
     * 已产出图片对象去重(PDF 常把同一张图在多页重复引用)
     */
    private final Set<COSStream> seenImages = new HashSet<>();
    private int imageSeq = 0;

    @Override
    public boolean support(DocumentSource source) {
        return DocumentMimes.PDF.equals(source.resolvedMime()) || "pdf".equals(source.extension());
    }

    @Override
    public void extract(DocumentSource source, TextSink sink) {
        Throwable error = null;
        boolean cancelled = false;
        try {
            document = Loader.loadPDF(source.path().toFile());
            int pages = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            for (int page = 1; page <= pages && !cancelled; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document);
                if (text != null && !text.isBlank()) {
                    if (!sink.onNext(TextChunk.text("第 " + page + " 页", text.stripTrailing()))) {
                        cancelled = true;
                        break;
                    }
                }
                // 该页内嵌图片(含 Form XObject 内嵌套的)
                PDPage pdPage = document.getPage(page - 1);
                if (!emitImages(pdPage.getResources(), sink, 0)) {
                    cancelled = true;
                    break;
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
     * 遍历资源里的 XObject,图片产出为图片块,Form XObject 递归下钻。返回 false 表示消费端要求取消。
     */
    private boolean emitImages(PDResources resources, TextSink sink, int depth) {
        if (resources == null || depth > MAX_FORM_DEPTH) return true;
        for (COSName name : resources.getXObjectNames()) {
            PDXObject xobject;
            try {
                xobject = resources.getXObject(name);
            } catch (Exception e) {
                continue;
            }
            if (xobject instanceof PDImageXObject image) {
                if (!seenImages.add(image.getCOSObject())) continue; // 去重
                if (!sink.onNext(toImageChunk(image))) return false;
            } else if (xobject instanceof PDFormXObject form) {
                if (!emitImages(form.getResources(), sink, depth + 1)) return false;
            }
        }
        return true;
    }

    private Chunk toImageChunk(PDImageXObject image) {
        try {
            RawImage raw = rawBytes(image);
            String fileName = "image-" + (++imageSeq) + "." + raw.ext();
            return ImageChunkSupport.land(raw.bytes(), fileName);
        } catch (Exception e) {
            return TextChunk.placeholder("图片", "[图片提取失败] " + e.getMessage());
        }
    }

    private record RawImage(byte[] bytes, String ext) {
    }

    /**
     * 取图片字节:JPEG 保留原始压缩流(无损、保原格式);其余格式统一无损编码为 PNG。
     */
    private RawImage rawBytes(PDImageXObject image) throws IOException {
        if ("jpg".equalsIgnoreCase(image.getSuffix())) {
            try (InputStream in = image.createInputStream(DCT_FILTERS)) {
                return new RawImage(in.readAllBytes(), "jpg");
            }
        }
        BufferedImage bi = image.getImage();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(bi, "png", out);
        return new RawImage(out.toByteArray(), "png");
    }

    @Override
    public void close() {
        seenImages.clear();
        if (document != null) {
            try {
                document.close();
            } catch (IOException ignored) {
            }
            document = null;
        }
    }
}
