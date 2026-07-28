package com.run.handler.tool.dto;

import com.run.handler.common.pojo.SimpleNodePojo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ToolNodeDTO extends SimpleNodePojo {
    private String label;
    private String runtime;
}
