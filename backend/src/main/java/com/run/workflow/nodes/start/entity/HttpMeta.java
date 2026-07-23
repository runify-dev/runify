package com.run.workflow.nodes.start.entity;

import com.run.workflow.nodes.response.pojo.ResponseNodeData;
import lombok.Data;

import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/28  13:17}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
public class HttpMeta {
    private String method;
    private String path;
    private String contentType;
    private List<Parameter> parameters;
    /**
     * 错误响应配置来源:"global"(默认,使用项目统一异常配置)/ "custom"(使用本节点 errorResponse)。
     */
    private String errorResponseSource;
    /**
     * 入参校验失败时的错误响应配置(errorResponseSource=custom 时生效),
     * 结构与响应节点一致(状态码/响应头/响应体)。
     * 校验错误信息会先写入开始节点上下文的 {@code error} 字段,响应体可通过
     * reference {@code [<开始节点id>, "error"]} 引用。
     * 为空时使用框架内置兜底默认:HTTP 200 + {@code {code:400, message:<错误信息>, data:null}}。
     */
    private ResponseNodeData errorResponse;

    @Data
    public static class Parameter {
        private String field;
        private String description;
        private Boolean required;
        private String location;
        private String type;
        private Boolean many;
    }


}
