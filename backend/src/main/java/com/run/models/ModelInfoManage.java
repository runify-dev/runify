package com.run.models;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/25  23:30}
 * {@code @Version 1.0}
 * {@code @注释: }
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ModelInfoManage {
    /**
     * 管理的模型
     */
    private List<ModelInfo> modelList;
    /**
     * 未收录的模型 使用defaultModelList进行获取模型
     */
    private List<ModelInfo> defaultModelList;

    private Map<ModelType, Map<String, ModelInfo>> modelDict;

    private Map<ModelType, ModelInfo> defaultModelDict;


    public static class Builder {
        private List<ModelInfo> modelList;
        private List<ModelInfo> defaultModelList;

        public Builder append(ModelInfo modelInfo, boolean isDefault) {
            this.modelList.add(modelInfo);
            if (isDefault) {
                this.defaultModelList.add(modelInfo);
            }
            return this;
        }

        public Builder append(List<ModelInfo> modelInfoList, boolean isDefault) {
            this.modelList.addAll(modelInfoList);
            if (isDefault) {
                this.defaultModelList.addAll(modelInfoList);
            }
            return this;
        }

        public ModelInfoManage build() {
            Map<ModelType, Map<String, ModelInfo>> modelDict = this.modelList.stream()
                    .collect(Collectors.groupingBy(ModelInfo::getModelType, Collectors.groupingBy(ModelInfo::getName, Collectors.reducing(null, (pre, next) -> next))));
            Map<ModelType, ModelInfo> defaultModelDict = this.defaultModelList.stream()
                    .collect(Collectors.groupingBy(ModelInfo::getModelType, Collectors.reducing(null, (pre, next) -> next)));
            return new ModelInfoManage(this.modelList, this.defaultModelList, modelDict, defaultModelDict);
        }
    }

    public static Builder builder() {
        Builder builder = new Builder();
        builder.modelList = new ArrayList<>();
        builder.defaultModelList = new ArrayList<>();
        return builder;
    }

}
