package com.run.workflow.nodes.cachequery.pojo;

import lombok.Data;

import java.util.List;

@Data
public class CacheQueryNodeData {
    private String cacheId;
    /**
     * reference / customize
     */
    private String keyLocation;
    private List<String> keyReference;
    private String key;
}
