package com.run.common.initialization;

import com.run.common.openapi.DocRoute;
import com.run.common.project.ProjectManage;
import com.run.common.util.RSAUtil;
import com.run.dao.entity.SystemSetting;
import com.run.dao.mapper.DatasourceMapper;
import com.run.dao.mapper.SystemSettingMapper;
import com.run.integrations.impl.weixin.WeixinPollerManager;
import com.run.integrations.impl.wecomstream.WecomStreamManager;
import com.run.route.*;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import javax.inject.Inject;
import java.security.KeyPair;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/27  20:02}
 * {@code @Version 1.0}
 * {@code @注释: }
 */

public class AppInitialization {
    @Inject
    public AppInitialization(UserRoute userRoute,
                             FileRoute fileRoute,
                             ChatRoute chatRoute,
                             ApplicationRoute applicationRoute,
                             DocRoute docRoute,
                             ModelRoute modelRoute,
                             ProjectRoute projectRoute,
                             ProcessorRoute processorRoute,
                             RoleRoute roleRoute,
                             DatasourceRoute datasourceRoute,
                             SkillRoute skillRoute,
                             KnowledgeRoute knowledgeRoute,
                             ConversationRoute conversationRoute,
                             IntegrationRoute integrationRoute,
                             IntegrationCallbackRoute integrationCallbackRoute,
                             WeixinPollerManager weixinPollerManager,
                             WecomStreamManager wecomStreamManager,
                             UIInitialization uiInitialization,
                             MigrationInitialization migrationInitialization,
                             SystemSettingMapper systemSettingMapper,
                             DatasourceMapper datasourceMapper) {
        userRoute.init();
        fileRoute.init();
        chatRoute.init();
        applicationRoute.init();
        docRoute.init();
        migrationInitialization.init();
        uiInitialization.init();
        skillRoute.init();
        knowledgeRoute.init();
        modelRoute.init();
        projectRoute.init();
        datasourceRoute.init();
        processorRoute.init();
        conversationRoute.init();
        roleRoute.init();
        integrationRoute.init();
        integrationCallbackRoute.init();
        ProjectManage.setDatasourceMapper(datasourceMapper);
        // 依赖加解密的初始化(如微信 poller 需解密集成配置)必须等 RSA 密钥就绪后再执行
        initRsa(systemSettingMapper)
                .onSuccess(v -> {
                    weixinPollerManager.startAll();
                    wecomStreamManager.startAll();
                })
                .onFailure(Throwable::printStackTrace);
    }

    /**
     * 初始化 RSA 密钥对(首次生成并持久化, 否则从 system_setting 加载), 完成后才可加解密
     */
    private Future<Void> initRsa(SystemSettingMapper systemSettingMapper) {
        return systemSettingMapper.getById("RSA")
                .compose(systemSetting -> {
                    if (systemSetting == null) {
                        KeyPair keyPair = RSAUtil.generateKeyPair();
                        SystemSetting instance = new SystemSetting();
                        instance.setType("RSA");
                        instance.setMeta(JsonObject.of("private", RSAUtil.getPrivateKey(keyPair),
                                "public", RSAUtil.getPublicKey(keyPair)));
                        return systemSettingMapper.save(instance).map(ok -> {
                            RSAUtil.setKeyPair(keyPair);
                            return null;
                        });
                    }
                    JsonObject meta = systemSetting.getMeta();
                    KeyPair keyPair = new KeyPair(
                            RSAUtil.loadPublicKey(meta.getString("public")),
                            RSAUtil.loadPrivateKey(meta.getString("private")));
                    RSAUtil.setKeyPair(keyPair);
                    return Future.succeededFuture();
                });
    }
}
