package com.run.integrations;

import com.run.integrations.impl.dingtalk.DingTalkProvider;
import com.run.integrations.impl.feishu.FeishuProvider;
import com.run.integrations.impl.wecomapp.WecomAppProvider;
import com.run.integrations.impl.wecomrobot.WecomRobotProvider;
import com.run.integrations.impl.wechat.WechatProvider;
import lombok.Getter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/20  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 第三方集成供应商枚举(枚举名对应 Integration.type)。新增平台在此注册独立实现。 }
 */
@Getter
public enum IntegrationProviderConstants {

    WECOM(new WecomAppProvider()),
    WECOM_ROBOT(new WecomRobotProvider()),
    FEISHU(new FeishuProvider()),
    DINGTALK(new DingTalkProvider()),
    WECHAT(new WechatProvider());

    IntegrationProviderConstants(IIntegrationProvider provider) {
        this.provider = provider;
    }

    private final IIntegrationProvider provider;

    public static IIntegrationProvider of(String type) {
        for (IntegrationProviderConstants c : values()) {
            if (c.name().equals(type)) {
                return c.provider;
            }
        }
        return null;
    }
}
