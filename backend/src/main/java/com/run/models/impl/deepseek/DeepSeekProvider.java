package com.run.models.impl.deepseek;

import com.run.models.*;
import com.run.models.impl.deepseek.credential.LLMCredential;
import com.run.models.impl.deepseek.model.LLM;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * {@code @Author:guguli}
 * {@code @Date: 2025/08/12  22:00}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class DeepSeekProvider implements IProvider {
    private static ModelInfoManage modelInfoManage;
    private static ProvideInfo provideInfo;

    {
        ModelInfo deepseek_reasoner = new ModelInfo("deepseek-reasoner", "", ModelType.LLM, new LLMCredential(), LLM.class);
        ModelInfo deepseek_chat = new ModelInfo("deepseek-chat", "", ModelType.LLM, new LLMCredential(), LLM.class);
        modelInfoManage = ModelInfoManage.builder()
                .append(deepseek_reasoner, true)
                .append(deepseek_chat, true).build();
        try {
            String file = Objects.requireNonNull(this.getClass().getResource("icon/deepseek.svg")).getFile();
            String icon = Files.readString(Paths.get(file), StandardCharsets.UTF_8);
            provideInfo = new ProvideInfo("deepseek_provider", "DeepSeek", icon);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ProvideInfo info() {
        return provideInfo;
    }

    @Override
    public ModelInfoManage getModelInfoManage() {
        return modelInfoManage;
    }
}
