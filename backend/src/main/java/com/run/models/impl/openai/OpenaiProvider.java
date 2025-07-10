package com.run.models.impl.openai;

import com.run.models.*;
import com.run.models.impl.openai.credential.LLMCredential;
import com.run.models.impl.openai.model.LLM;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
        try {
            String file = Objects.requireNonNull(this.getClass().getResource("icon/openai.svg")).getFile();
            String icon = FileUtils.readFileToString(new File(file), StandardCharsets.UTF_8);
            provideInfo = new ProvideInfo(ModelType.LLM.name(), "openai_provider", icon);
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
