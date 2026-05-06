package com.run.handler.application.vo;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.run.common.pojo.File;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/30  16:17}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class QuestionContent {
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private String content;
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<File> images;
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<File> videos;
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<File> files;
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<String> texts;
}
