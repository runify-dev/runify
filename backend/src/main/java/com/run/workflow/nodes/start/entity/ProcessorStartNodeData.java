package com.run.workflow.nodes.start.entity;

import com.run.common.constants.ProcessorProtocolConstants;
import lombok.Data;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/25  23:40}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
public class ProcessorStartNodeData<HttpMeta> {
    private HttpMeta meta;
    private ProcessorProtocolConstants protocol;
}
