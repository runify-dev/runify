package com.run.dao.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.run.RunApplication;
import com.run.common.config.AppConfig;
import com.run.common.constants.DatabaseType;
import com.run.common.util.JacksonUtils;
import com.run.common.util.RSAUtil;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.Model;
import io.vertx.core.Future;
import io.vertx.core.json.impl.JsonUtil;
import io.vertx.sqlclient.Pool;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.beanutils.BeanUtils;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/13  15:57}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ModelMapper extends BaseMapper<Model> {

    @NoArgsConstructor
    @Getter
    @Setter
    public static class ModelDetail extends Model {
        private Map<String, Object> credentialData;

    }

    public ModelMapper(Pool client, DatabaseType dbType) {
        super(client, dbType, Model.class);
    }

    @Inject
    public ModelMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }

    @Override
    public Future<Model> getById(String id) {
        return super.getById(id).compose(model -> {
            String credential = model.getCredential();
            Map<String, Object> stringObjectMap = new HashMap<>();
            try {
                stringObjectMap = JacksonUtils.fromJson(RSAUtil.decrypt(credential), new TypeReference<Map<String, Object>>() {

                });
            } catch (Exception _) {

            }

            ModelDetail modelDetail = new ModelDetail();
            modelDetail.setCredentialData(stringObjectMap);
            try {
                BeanUtils.copyProperties(modelDetail, model);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            modelDetail.setCredential(null);
            return Future.succeededFuture(modelDetail);
        });
    }
}
