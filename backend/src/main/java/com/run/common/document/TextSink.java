package com.run.common.document;

import java.util.Optional;

/**
 * 文本消费端。由调用方(如 ReadFileNode / 知识库上传)实现,决定提取出的文本往哪去。
 * <p>
 * 生命周期约定:
 * <ul>
 *     <li>处理器每提取到一块就调用一次 {@link #onNext(Chunk)},块类型见 {@link ChunkType}
 *     ({@link TextChunk} / {@link ImageChunk});</li>
 *     <li>{@link #onNext} 返回 {@code false} 表示消费端要求停止(取消),处理器应立即中止且
 *     <b>不再</b>调用 {@link #onComplete};</li>
 *     <li>自然读完 → {@code onComplete(Optional.empty())};</li>
 *     <li>提取中途异常 → {@code onComplete(Optional.of(e))};</li>
 *     <li>{@code onComplete} 是语义终止信号,用于决定收尾动作(写结果 / 提交入库 vs 丢弃回滚);
 *     资源释放不在这里,由处理器的 {@link DocumentProcessor#close()} 负责。</li>
 * </ul>
 */
public interface TextSink {

    /**
     * 消费一块内容。消费端按 {@link Chunk#getType()} 或 {@code instanceof} 分流处理。
     *
     * @param chunk 提取出的内容块({@link TextChunk} / {@link ImageChunk})
     * @return {@code true} 继续提取; {@code false} 请求停止(取消)
     */
    boolean onNext(Chunk chunk);

    /**
     * 提取终止时回调一次。
     *
     * @param error 为空表示正常读完;有值表示提取中途出错
     */
    void onComplete(Optional<Throwable> error);
}
