package com.run.models;

import com.run.models.impl.deepseek.DeepSeekProvider;
import com.run.models.impl.mimo.MimoProvider;
import com.run.models.impl.openai.OpenaiProvider;
import lombok.Getter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/26  22:31}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
public enum ModelProvideConstants {


    openai_provider(new OpenaiProvider()),
    deepseek_provider(new DeepSeekProvider()),
    mimo_provider(new MimoProvider());

    ModelProvideConstants(IProvider provider) {
        this.provider = provider;
    }

    private IProvider provider;

}
