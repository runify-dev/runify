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

    private Map<ModelType, Map<String, ModelInfo>> modelDict;

    private Map<ModelType, ModelInfo> defaultModelDict;


    public static class Builder {
        private List<ModelInfo> modelList;

        public Builder append(ModelInfo modelInfo, Boolean isDefault) {
            this.modelList.add(modelInfo);
            modelInfo.setIsDefault(isDefault);
            return this;
        }

        public Builder append(List<ModelInfo> modelInfoList, boolean isDefault) {
            this.modelList.addAll(modelInfoList);
            for (ModelInfo modelInfo : modelInfoList) {
                modelInfo.setIsDefault(isDefault);
            }
            return this;
        }

        public ModelInfoManage build() {
            Map<ModelType, Map<String, ModelInfo>> modelDict = this.modelList.stream()
                    .collect(Collectors.groupingBy(ModelInfo::getModelType, Collectors.groupingBy(ModelInfo::getName, Collectors.reducing(null, (pre, next) -> next))));
            Map<ModelType, ModelInfo> defaultModelDict = this.modelList.stream()
                    .filter(ModelInfo::getIsDefault)
                    .collect(Collectors.groupingBy(ModelInfo::getModelType, Collectors.reducing(null, (pre, next) -> next)));
            return new ModelInfoManage(this.modelList, modelDict, defaultModelDict);
        }
    }

    public static Builder builder() {
        Builder builder = new Builder();
        builder.modelList = new ArrayList<>();
        return builder;
    }

}
