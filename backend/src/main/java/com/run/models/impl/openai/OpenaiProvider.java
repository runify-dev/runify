package com.run.models.impl.openai;

import com.run.models.*;
import com.run.models.impl.openai.credential.LLMCredential;
import com.run.models.impl.openai.model.LLM;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/25  22:52}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class OpenaiProvider implements IProvider {
    private static ModelInfoManage modelInfoManage;
    private static ProvideInfo provideInfo;

    {
        ModelInfo deepseek = new ModelInfo("deepseek-r1", "", ModelType.LLM, new LLMCredential(), LLM.class);
        ModelInfo qwenMax = new ModelInfo("qwen-max", "", ModelType.LLM, new LLMCredential(), LLM.class);
        modelInfoManage = ModelInfoManage.builder()
                .append(deepseek, true)
                .append(qwenMax, false).build();
        String filePath = "com/run/models/impl/openai/icon/openai.svg";
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
