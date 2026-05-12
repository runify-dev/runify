package com.run.common.config;

import lombok.Data;

import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/20  23:33}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
public class Search {
    /**
     * local /elasticsearch
     */
    private SearchType type;
    private List<String> hosts;
    private String username;
    private String password;
}
