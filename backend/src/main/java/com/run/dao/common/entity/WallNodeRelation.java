package com.run.dao.common.entity;

import java.util.Map;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/18  21:59}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface WallNodeRelation<T, N> {
    T apply(UUID id, UUID ancestorId, UUID descendantId, Integer dept);

    N build(UUID id, UUID parentId, String type, String name);

    UUID getAncestorId(T t);

    UUID getDescendantId(T t);

    Integer getDepth(T t);

    UUID getParentId(N n);

    void setParentId(N n, UUID parentId);

    UUID getId(N n);

    String getName(N n);

    Map<String,String> getNamePrefixMap();


}
