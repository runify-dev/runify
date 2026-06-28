package com.run.integrations;

import java.util.List;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/28  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 集成平台类型目录(唯一真源): 类型/标签/回调路径/认证方式/凭证字段。
 * 后端出列表, 前端按 authMode 渲染(credential=凭证表单, qrcode=扫码登录)。新增平台只改这里, 前端无需同步。
 * callbackPath 为空表示无需公网回调(自驱动连接, 如个人微信长轮询、企业微信长连接)。 }
 */
public final class IntegrationTypeCatalog {

    private IntegrationTypeCatalog() {
    }

    private static Map<String, Object> field(String field, String label, String placeholder, boolean secret) {
        return Map.of("field", field, "label", label, "placeholder", placeholder, "secret", secret);
    }

    private static Map<String, Object> type(String type, String label, String callbackPath,
                                             String authMode, List<Map<String, Object>> fields) {
        return Map.of("type", type, "label", label, "callbackPath", callbackPath,
                "authMode", authMode, "fields", fields);
    }

    public static List<Map<String, Object>> list() {
        return List.of(
                type("WECOM", "企业微信应用", "/integration/{id}/callback", "credential", List.of(
                        field("corpId", "CorpID", "企业ID", false),
                        field("agentId", "AgentID", "应用 AgentId", false),
                        field("secret", "Secret", "应用 Secret", true),
                        field("token", "Token", "回调 Token", true),
                        field("aesKey", "EncodingAESKey", "回调 EncodingAESKey", true))),
                type("WECOM_ROBOT", "企业微信机器人", "/integration/{id}/callback", "credential", List.of(
                        field("token", "Token", "回调 Token", true),
                        field("aesKey", "EncodingAESKey", "回调 EncodingAESKey", true))),
                type("WECOM_STREAM", "企业微信机器人(长连接)", "", "credential", List.of(
                        field("botId", "BotID", "智能机器人 BotID", false),
                        field("secret", "Secret", "智能机器人 Secret", true))),
                type("FEISHU", "飞书", "/integration/{id}/callback", "credential", List.of(
                        field("appId", "App ID", "飞书应用 App ID", false),
                        field("appSecret", "App Secret", "飞书应用 App Secret", true),
                        field("verifyToken", "Verification Token", "事件订阅 Verification Token", true),
                        field("encryptKey", "Encrypt Key", "事件订阅 Encrypt Key", true))),
                type("DINGTALK", "钉钉", "/integration/{id}/callback", "credential", List.of(
                        field("appKey", "AppKey", "机器人 AppKey/ClientId", false),
                        field("appSecret", "AppSecret", "机器人 AppSecret(验签用)", true))),
                type("WEIXIN", "微信(个人号)", "", "qrcode", List.of()),
                type("WECHAT", "微信公众号", "/integration/{id}/callback", "credential", List.of(
                        field("appId", "AppID", "公众号 AppID", false),
                        field("appSecret", "AppSecret", "公众号 AppSecret", true),
                        field("token", "Token", "服务器配置 Token", true),
                        field("aesKey", "EncodingAESKey", "消息加解密密钥", true)))
        );
    }
}
