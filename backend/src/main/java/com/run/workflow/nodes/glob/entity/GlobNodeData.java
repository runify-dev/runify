package com.run.workflow.nodes.glob.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GlobNodeData {
    private String location;
    private List<String> reference;

    private String patternLocation;
    private List<String> patternReference;
    private String pattern;

    private String pathLocation;
    private List<String> pathReference;
    private String path;

    private String maxResultsLocation;
    private List<String> maxResultsReference;
    private Integer maxResults;
}
