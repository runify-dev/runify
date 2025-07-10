package com.run.common.openai.request.message;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/17  22:27}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
public class Audio {
    /**
     * 关于模型先前音频响应的数据。
     * [了解更多](https://platform.openai.com/docs/guides/audio).
     */
    private String id;
}
