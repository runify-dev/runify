package com.run.handler.project.vo;

import com.run.common.query.annotations.QueryParams;
import com.run.common.query.constants.LocationConstants;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/1/1  22:33}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateDatabaseCollectionPoolVO {

    private String name;

    private String desc;

    private String protocol;

    private JsonObject meta;
}
