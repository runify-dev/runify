package com.run.datasources.impl.redis.credential;

import com.run.common.forms.field.NumberInputField;
import com.run.common.forms.field.PasswordInputField;
import com.run.common.forms.field.TextInputField;
import com.run.common.forms.label.TextLabel;
import com.run.common.util.CommonUtils;
import com.run.datasources.BaseDatasourceCredential;

import java.util.HashMap;
import java.util.Map;

/**
 * Redis 数据源凭证
 */
public class RedisCredential extends BaseDatasourceCredential {

    private final TextInputField host = new TextInputField(new TextLabel("主机地址"), true, "localhost");

    private final NumberInputField port = new NumberInputField(new TextLabel("端口"), true, 6379, 1, 65535);

    private final PasswordInputField password = new PasswordInputField(new TextLabel("密码"), false, "");

    private final NumberInputField database = new NumberInputField(new TextLabel("数据库"), true, 0, 0, 15);

    @Override
    public void validateCredential(Map<String, Object> credential) {
        super.validate(credential);
    }

    @Override
    public Map<String, Object> encryption(Map<String, Object> credential) {
        HashMap<String, Object> result = new HashMap<>(credential);
        String password = (String) credential.get("password");
        if (password != null && !password.isEmpty()) {
            result.put("password", CommonUtils.encryption(password));
        }
        return result;
    }
}
