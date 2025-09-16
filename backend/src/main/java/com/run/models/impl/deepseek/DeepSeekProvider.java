package com.run.models.impl.deepseek;

import com.run.models.*;
import com.run.models.impl.deepseek.credential.LLMCredential;
import com.run.models.impl.deepseek.model.LLM;

import java.io.IOException;
import java.io.InputStream;
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
                .append(deepseek_chat, false).build();
        String filePath = "com/run/models/impl/deepseek/icon/deepseek.svg";
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(filePath)) {
            if (is == null) {
                throw new RuntimeException("Resource not found: " + filePath);
            }
            String icon = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            // 使用 icon
            provideInfo = new ProvideInfo("openai_provider", "OpenAI", icon);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource: " + filePath, e);
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
