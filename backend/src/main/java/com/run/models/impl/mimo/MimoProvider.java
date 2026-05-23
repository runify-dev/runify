package com.run.models.impl.mimo;

import com.run.common.util.CommonUtils;
import com.run.models.*;
import com.run.models.impl.mimo.credential.LLMCredential;
import com.run.models.impl.mimo.model.LLM;


/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/25  22:52}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class MimoProvider implements IProvider {
    private static final ModelInfoManage modelInfoManage;
    private static final ProvideInfo provideInfo;

    static {
        ModelInfo mimo_2_5_pro = new ModelInfo("mimo-v2.5-pro", "", ModelType.LLM, new LLMCredential(), LLM.class);
        ModelInfo mimo_2_5 = new ModelInfo("mimo-v2.5", "", ModelType.LLM, new LLMCredential(), LLM.class);
        modelInfoManage = ModelInfoManage.builder()
                .append(mimo_2_5_pro, true)
                .append(mimo_2_5, false).build();
        String icon = CommonUtils.getFileContent("com/run/models/impl/mimo/icon/momo.svg");
        provideInfo = new ProvideInfo("mimo_provider", "Mimo", icon);
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
