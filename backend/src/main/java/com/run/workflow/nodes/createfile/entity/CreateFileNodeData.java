package com.run.workflow.nodes.createfile.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateFileNodeData {
    private String location;
    private List<String> reference;

    private String pathLocation;
    private List<String> pathReference;
    private String path;

    private String contentLocation;
    private List<String> contentReference;
    private String content;
}
