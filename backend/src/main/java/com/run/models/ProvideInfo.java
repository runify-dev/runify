package com.run.models;

import lombok.*;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/25  22:54}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ProvideInfo {
    /**
     * 供应商字符串
     */
    private String provider;
    /**
     * 供应商名称
     */
    private String name;
    /**
     * 供应商icon
     */
    private String icon;
}
