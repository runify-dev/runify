package com.run.handler.knowledge.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EditKnowledge {
    private String name;
    private String icon;
    private String desc;
}
