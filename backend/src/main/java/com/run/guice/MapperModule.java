package com.run.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.run.dao.common.entity.BaseEntity;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.mapper.*;

import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/13  16:02}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class MapperModule extends AbstractModule {
    private List<Class<? extends BaseMapper<? extends BaseEntity<? extends BaseEntity<?>>>>> mapperList =
            List.of(ModelMapper.class, UserMapper.class
                    ,   FileMapper.class, KnowledgeMapper.class);

    @Override
    protected void configure() {
        for (Class<? extends BaseMapper<? extends BaseEntity<? extends BaseEntity<?>>>> aClass : mapperList) {
            bind(Key.get(aClass));
        }
    }
}
