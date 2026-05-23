package com.run.models.impl.mimo.credential;

import com.run.common.forms.BaseForm;
import com.run.common.forms.field.PasswordInputField;
import com.run.common.forms.field.TextInputField;
import com.run.common.forms.label.TextLabel;
import com.run.common.util.CommonUtils;
import com.run.models.BaseModelCredential;
import com.run.models.IProvider;
import com.run.models.impl.mimo.model.LLM;


import java.util.HashMap;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/22  20:08}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class LLMCredential extends BaseForm implements BaseModelCredential {

    private final TextInputField baseUrl = new TextInputField(new TextLabel("baseUrl"), true, "https://token-plan-sgp.xiaomimimo.com/v1");

    private final PasswordInputField apiKey = new PasswordInputField(new TextLabel("apiKey"), true, "");


    @Override
    public void validate(Map<String, Object> modelCredential, String modelType, String modelName, IProvider provider, Map<String, Object> other) {
        super.validate(modelCredential);
        LLM model = provider.getModel(modelType, modelName, modelCredential, other, LLM.class);
        model.validate(modelType, modelName, modelCredential, other);
    }

    public Map<String, Object> encryption(Map<String, Object> credential) {
        HashMap<String, Object> result = new HashMap<>(credential);
        result.put("apiKey", CommonUtils.encryption((String) credential.get("apiKey")));
        return result;
    }

}
