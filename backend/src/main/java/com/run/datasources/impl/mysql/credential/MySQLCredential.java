package com.run.datasources.impl.mysql.credential;

import com.run.common.forms.field.NumberInputField;
import com.run.common.forms.field.PasswordInputField;
import com.run.common.forms.field.TextInputField;
import com.run.common.forms.label.TextLabel;
import com.run.common.util.CommonUtils;
import com.run.datasources.BaseDatasourceCredential;

import java.util.HashMap;
import java.util.Map;

/**
 * MySQL 数据源凭证
 */
public class MySQLCredential extends BaseDatasourceCredential {

    private final TextInputField host = new TextInputField(new TextLabel("主机地址"), true, "localhost");

    private final NumberInputField port = new NumberInputField(new TextLabel("端口"), true, 3306, 1, 65535);

    private final TextInputField database = new TextInputField(new TextLabel("数据库名"), true, "");

    private final TextInputField user = new TextInputField(new TextLabel("用户名"), true, "");

    private final PasswordInputField password = new PasswordInputField(new TextLabel("密码"), true, "");

    private final NumberInputField maxSize = new NumberInputField(new TextLabel("最大连接数"), true, 10, 1, 100);

    @Override
    public void validateCredential(Map<String, Object> credential) {
        super.validate(credential);
    }

    @Override
    public Map<String, Object> encryption(Map<String, Object> credential) {
        HashMap<String, Object> result = new HashMap<>(credential);
        result.put("password", CommonUtils.encryption((String) credential.get("password")));
        return result;
    }
}
