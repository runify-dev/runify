package com.run.handler.tree.pojo;


import com.run.common.validator.Group;
import io.vertx.core.json.JsonObject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/6  17:01}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
public class NodePojo {
    /**
     * 父id
     */
    private String parentId;
    /**
     * 类型
     */
    @Pattern(regexp = "folder", message = "节点类型:folder|file|knowledge|application", groups = Group.Create.class)
    @NotBlank(message = "节点类型不能为空", groups = Group.Create.class)
    private String type;
    @Pattern(regexp = "folder", message = "节点子类型:folder|file|knowledge|application|markdown", groups = Group.Create.class)
    @NotBlank(message = "节点子类型不能为空", groups = Group.Create.class)
    private String subtype;
    /**
     * 所属
     */
    @Pattern(regexp = "knowledge|application", message = "节点所属必须为knowledge|application", groups = Group.Create.class)
    @NotBlank(message = "节点所属不能为空", groups = Group.Create.class)
    private String source;

    @NotBlank(message = "节点名称不能为空", groups = Group.Create.class)
    private String name;

    @NotNull(message = "元数据", groups = Group.Create.class)
    private JsonObject meta;


}
