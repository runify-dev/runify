package com.run.handler.conversation.vo;

import com.run.common.query.annotations.QueryParams;
import lombok.Data;

@Data
public class EmbedVO {
    @QueryParams(name = "host")
    private String host;
    @QueryParams(name = "protocol")
    private String protocol;
    @QueryParams(name = "port")
    private String port;
}
