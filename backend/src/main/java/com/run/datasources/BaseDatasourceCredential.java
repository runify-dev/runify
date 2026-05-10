package com.run.datasources;

import com.run.common.forms.BaseForm;

import java.util.List;
import java.util.Map;

/**
 * 数据源凭证基类
 */
public abstract class BaseDatasourceCredential extends BaseForm {

    /**
     * 验证连接配置
     *
     * @param credential 连接配置
     */
    public abstract void validateCredential(Map<String, Object> credential);

    /**
     * 加密敏感数据
     *
     * @param credential 原始配置
     * @return 加密后的配置
     */
    public abstract Map<String, Object> encryption(Map<String, Object> credential);

    /**
     * 获取表单定义列表
     *
     * @param keywords 关键字
     * @return 表单定义
     */
    public List<Map<String, Object>> toFormList(Map<String, Object> keywords) {
        return super.toFormList(keywords);
    }
}
