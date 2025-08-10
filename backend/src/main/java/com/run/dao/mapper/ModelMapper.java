package com.run.dao.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.run.RunApplication;
import com.run.common.config.AppConfig;
import com.run.common.constants.DatabaseType;
import com.run.common.util.CommonUtils;
import com.run.common.util.JacksonUtils;
import com.run.common.util.RSAUtil;
import com.run.dao.common.convert.BaseConvert;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.Model;
import com.run.models.ModelInfo;
import com.run.models.ModelProvideConstants;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.impl.JsonUtil;
import io.vertx.sqlclient.Pool;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;

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

    public Future<Map<String, Object>> getResourceById(String id) {
        return getById(id).compose(model -> {
            if (model == null) {
                return null;
            }
            Map<String, Object> result = JacksonUtils.convert(model, new TypeReference<HashMap<String, Object>>() {
            });
            result.put("modelParameterForm", model.getModelParameterForm());
            Map<String, Object> credential = model.decrypt().getMap();
            if (StringUtils.isNoneEmpty(model.getProvider())) {
                ModelInfo modelInfo = ModelProvideConstants.valueOf(model.getProvider()).getProvider().getModelInfo(model.getModelType(),
                        model.getModelName());
                Map<String, Object> encryption = modelInfo.getCredential().encryption(credential);
                result.put("credential", encryption);
            } else {
                result.put("credential", Map.of());
            }
            return Future.succeededFuture(result);
        });
    }
}
