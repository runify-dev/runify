package com.run.common.project.executor;

import com.run.common.project.ProjectManage;
import com.run.dao.entity.Processor;
import lombok.Getter;

import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/29  22:57}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
public abstract class ProcessorExecutor {
    protected UUID id;
    protected Processor processor;
    protected ProjectManage.ProjectExecutor projectExecutor;

    public ProcessorExecutor(Processor processor, ProjectManage.ProjectExecutor projectExecutor) {
        this.id = processor.getId();
        this.processor = processor;
        this.projectExecutor = projectExecutor;
    }

    public Boolean unDeploy() {
        this.id = null;
        this.processor = null;
        this.projectExecutor = null;
        return Boolean.TRUE;
    }

    public abstract Boolean deploy();

}