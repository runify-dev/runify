package com.run.common.initialization;

import com.run.common.openapi.DocRoute;
import com.run.common.project.ProjectManage;
import com.run.common.util.RSAUtil;
import com.run.dao.entity.SystemSetting;
import com.run.dao.mapper.DatasourceMapper;
import com.run.dao.mapper.SystemSettingMapper;
import com.run.route.*;
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
                             NoteRoute noteRoute,
                             DocRoute docRoute,
                             ModelRoute modelRoute,
                             ProjectRoute projectRoute,
                             ProcessorRoute processorRoute,
                             RoleRoute roleRoute,
                             DatasourceRoute datasourceRoute,
                             SkillRoute skillRoute,
                             ConversationRoute conversationRoute,
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
        noteRoute.init();
        skillRoute.init();
        modelRoute.init();
        projectRoute.init();
        datasourceRoute.init();
        processorRoute.init();
        conversationRoute.init();
        roleRoute.init();
        ProjectManage.setDatasourceMapper(datasourceMapper);
        systemSettingMapper.getById("RSA").onSuccess(systemSetting -> {
            if (systemSetting == null) {
                KeyPair keyPair = RSAUtil.generateKeyPair();
                String privateKey = RSAUtil.getPrivateKey(keyPair);
                String publicKey = RSAUtil.getPublicKey(keyPair);
                SystemSetting instance = new SystemSetting();
                instance.setType("RSA");
                instance.setMeta(JsonObject.of("private", privateKey, "public", publicKey));
                systemSettingMapper.save(instance).onSuccess(ok -> {
                    RSAUtil.setKeyPair(keyPair);
                });
            } else {
                JsonObject meta = systemSetting.getMeta();
                String privateKey = meta.getString("private");
                String publicKey = meta.getString("public");
                KeyPair keyPair = new KeyPair(RSAUtil.loadPublicKey(publicKey), RSAUtil.loadPrivateKey(privateKey));
                RSAUtil.setKeyPair(keyPair);
            }
        });

    }
}
