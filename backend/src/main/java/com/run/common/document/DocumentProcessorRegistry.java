package com.run.common.document;

import com.run.common.document.impl.DocProcessor;
import com.run.common.document.impl.DocxProcessor;
import com.run.common.document.impl.PdfProcessor;
import com.run.common.document.impl.ZipProcessor;

import java.util.List;
import java.util.function.Supplier;

/**
 * 文档处理器注册表。按注册顺序为给定来源挑选第一个 {@link DocumentProcessor#support 支持}的处理器,
 * 并返回一个<b>新实例</b>(处理器有状态,不可复用)。
 * <p>
 * 顺序敏感:DOCX 需排在 ZIP 之前——DOCX 本质是 zip 容器,靠 mime/扩展名区分,顺序可再加一层保险。
 */
public class DocumentProcessorRegistry {

    /**
     * 每种处理器一个工厂,{@code support} 判定用临时实例(不打开资源,开销极低)。
     */
    private final List<Supplier<DocumentProcessor>> factories = List.of(
            PdfProcessor::new,
            DocxProcessor::new,
            DocProcessor::new,
            ZipProcessor::new
    );

    private static final DocumentProcessorRegistry INSTANCE = new DocumentProcessorRegistry();

    public static DocumentProcessorRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * 为来源挑选处理器。命中返回一个新建实例(调用方负责 {@code close});未命中返回 {@code null}。
     */
    public DocumentProcessor pick(DocumentSource source) {
        for (Supplier<DocumentProcessor> factory : factories) {
            DocumentProcessor processor = factory.get();
            if (processor.support(source)) {
                return processor;
            }
        }
        return null;
    }

    /**
     * 是否存在能处理该来源的处理器。
     */
    public boolean supports(DocumentSource source) {
        return factories.stream().anyMatch(f -> f.get().support(source));
    }
}
