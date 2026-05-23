package com.run.models.impl.deepseek;

import com.run.common.util.CommonUtils;
import com.run.models.*;
import com.run.models.impl.deepseek.credential.LLMCredential;
import com.run.models.impl.deepseek.model.LLM;

/**
 * {@code @Author:guguli}
 * {@code @Date: 2025/08/12  22:00}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class DeepSeekProvider implements IProvider {
    private static final ModelInfoManage modelInfoManage;
    private static final ProvideInfo provideInfo;

    static {
        ModelInfo deepseek_reasoner = new ModelInfo("deepseek-v4-flash", "", ModelType.LLM, new LLMCredential(), LLM.class);
        ModelInfo deepseek_chat = new ModelInfo("deepseek-v4-pro", "", ModelType.LLM, new LLMCredential(), LLM.class);
        modelInfoManage = ModelInfoManage.builder()
                .append(deepseek_reasoner, true)
                .append(deepseek_chat, false).build();
        String icon = CommonUtils.getFileContent("com/run/models/impl/deepseek/icon/deepseek.svg");
        provideInfo = new ProvideInfo("deepseek_provider", "Deepseek", icon);
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
