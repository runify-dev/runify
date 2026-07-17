package com.run.common.document;

/**
 * 文档文本提取处理器。按文件类型(PDF / DOCX / ZIP ...)分别实现,流式地把二进制文档转成文本。
 * <p>
 * 一个处理器实例对应一次提取,内部可持有游标与资源句柄,因此<b>非线程安全</b>,应每个文件新建一个,
 * 由 {@link DocumentProcessorRegistry} 负责选择与创建。
 * <p>
 * 典型用法(资源释放交给 try-with-resources 兜底):
 * <pre>{@code
 * try (DocumentProcessor p = registry.pick(source)) {
 *     if (p != null) {
 *         p.extract(source, sink);   // 流式产出, 收尾由 sink.onComplete 决定
 *     }
 * }                                  // 无论正常/异常/取消, close() 都会执行
 * }</pre>
 */
public interface DocumentProcessor extends AutoCloseable {

    /**
     * 是否支持该文件。应为廉价判断(按 mime / 扩展名),<b>不要</b>在这里打开资源。
     */
    boolean support(DocumentSource source);

    /**
     * 流式提取文本。每提取一块调用 {@link TextSink#onNext(Chunk)};
     * 正常读完调用 {@code sink.onComplete(Optional.empty())};
     * 中途异常调用 {@code sink.onComplete(Optional.of(e))};
     * 若 {@code onNext} 返回 {@code false}(取消)则立即中止且不调用 {@code onComplete}。
     */
    void extract(DocumentSource source, TextSink sink);

    /**
     * 释放提取过程中打开的句柄(如 PDDocument / 压缩流)。必须<b>幂等</b>,可被安全地多次调用。
     */
    @Override
    void close();
}
