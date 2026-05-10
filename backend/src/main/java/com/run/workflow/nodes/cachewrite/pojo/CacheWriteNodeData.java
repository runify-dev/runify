package com.run.workflow.nodes.cachewrite.pojo;

import lombok.Data;

import java.util.List;

@Data
public class CacheWriteNodeData {
    private String cacheId;
    /**
     * reference / customize
     */
    private String keyLocation;
    private List<String> keyReference;
    private String key;
    /**
     * reference / customize
     */
    private String valueLocation;
    private List<String> valueReference;
    private String value;
    private Long ttl;
}
