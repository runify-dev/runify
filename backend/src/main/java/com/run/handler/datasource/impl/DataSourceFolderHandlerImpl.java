package com.run.handler.datasource.impl;

import com.run.dao.entity.DataSourceFolder;
import com.run.dao.entity.DataSourceRelation;
import com.run.dao.mapper.DataSourceFolderMapper;
import com.run.dao.mapper.DataSourceRelationMapper;
import com.run.handler.common.impl.FolderHandlerImpl;
import com.run.handler.datasource.IDataSourceFolderHandler;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/1/1  22:30}
 * {@code @Version 1.0}
 * {@code @注释: 数据源文件夹处理器实现}
 */
public class DataSourceFolderHandlerImpl extends FolderHandlerImpl<DataSourceFolder, DataSourceRelation, DataSourceFolderMapper, DataSourceRelationMapper> implements IDataSourceFolderHandler {

    @Inject
    public DataSourceFolderHandlerImpl(DataSourceFolderMapper dataSourceFolderMapper, DataSourceRelationMapper dataSourceRelationMapper) {
        super(dataSourceFolderMapper, dataSourceRelationMapper);
    }

    @Override
    protected UUID getParentId(DataSourceFolder resource) {
        return resource.getParentId();
    }

    @Override
    protected void setName(DataSourceFolder resource, String name) {
        resource.setName(name);
    }

    @Override
    protected String getName(DataSourceFolder resource) {
        return resource.getName();
    }

    @Override
    protected DataSourceRelation newRelation(UUID id, UUID ancestorId, UUID descendantId, Integer dept) {
        return new DataSourceRelation(id, ancestorId, descendantId, dept);
    }

    @Override
    protected DataSourceFolder newFolder(UUID id, UUID parentId, String name, String desc) {
        LocalDateTime now = LocalDateTime.now();
        return new DataSourceFolder(id, parentId, name, desc, now, now);
    }

    @Override
    protected UUID getAncestorId(DataSourceRelation relation) {
        return relation.getAncestorId();
    }

    @Override
    protected Integer getDepth(DataSourceRelation relation) {
        return relation.getDepth();
    }
}
