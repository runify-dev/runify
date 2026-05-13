package com.run.workflow.nodes.grep.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GrepNodeData {
    private String location;
    private List<String> reference;

    private String patternLocation;
    private List<String> patternReference;
    private String pattern;

    private String pathLocation;
    private List<String> pathReference;
    private String path;

    private String filePatternLocation;
    private List<String> filePatternReference;
    private String filePattern;

    private String contextLinesLocation;
    private List<String> contextLinesReference;
    private Integer contextLines;

    private String maxResultsLocation;
    private List<String> maxResultsReference;
    private Integer maxResults;
}
