package com.run.workflow.nodes.contextmanage.service;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;

/**
 * token 计量（jtokkit 本地计算，零调用成本）。
 * <p>
 * 只做决策信号不做账单，cl100k 对国产模型（deepseek 等）按近似处理；
 * 需要 o200k 时通过 {@link #of(String)} 切换。
 */
public final class TokenCounter {

    private final Encoding encoding;

    private TokenCounter(Encoding encoding) {
        this.encoding = encoding;
    }

    public static TokenCounter of(String encodingName) {
        EncodingType type = "o200k".equalsIgnoreCase(encodingName)
                ? EncodingType.O200K_BASE
                : EncodingType.CL100K_BASE;
        return new TokenCounter(Encodings.newDefaultEncodingRegistry().getEncoding(type));
    }

    public int count(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return encoding.countTokens(text);
    }
}
