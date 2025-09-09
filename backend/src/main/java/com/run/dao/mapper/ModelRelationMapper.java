package com.run.dao.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.run.common.config.AppConfig;
import com.run.common.util.JacksonUtils;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.Model;
import com.run.dao.entity.ModelRelation;
import com.run.models.ModelInfo;
import com.run.models.ModelProvideConstants;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jooq.SQLDialect;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/13  15:57}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ModelRelationMapper extends BaseMapper<ModelRelation> {
    @Inject
    public ModelRelationMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
