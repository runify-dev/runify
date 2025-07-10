package com.run.handler.node.pojo;

import com.run.common.exception.ApiException;
import com.run.common.validator.Group;
import io.vertx.core.MultiMap;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/13  19:23}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
public class QueryNodePojo {
    private String parentId;
    /**
     * 所属
     */
    @Pattern(regexp = "knowledge|application", message = "节点所属必须为knowledge|application", groups = Group.Create.class)
    @NotBlank(message = "节点所属不能为空", groups = Group.Create.class)
    private String source;
    /**
     * 节点类型
     */
    private String type;
    /**
     * 节点名称
     */
    private String name;
    /**
     * 深度
     */
    private Integer depth;
    /**
     * 加星
     */
    private Boolean star;
    /**
     * 分享
     */
    private Boolean share;

    public QueryNodePojo(MultiMap queryParams) {
        this.parentId = queryParams.get("parentId");
        this.source = queryParams.get("source");
        this.name = queryParams.get("name");
        this.type = queryParams.get("type");
        try {
            this.star = StringUtils.isNotEmpty(queryParams.get("star")) ? Boolean.valueOf(queryParams.get("star")) : null;
        } catch (Exception e) {
            throw new ApiException(500, "加星字段类型错误");
        }
        try {
            this.share = StringUtils.isNotEmpty(queryParams.get("share")) ? Boolean.valueOf(queryParams.get("share")) : null;
        } catch (Exception e) {
            throw new ApiException(500, "分享字段类型错误");
        }
        try {
            if (StringUtils.isNotEmpty(queryParams.get("depth"))) {
                this.depth = Integer.valueOf(queryParams.get("depth"));
            }
        } catch (Exception e) {
            throw new ApiException(500, "深度字段类型错误");
        }

    }
}
