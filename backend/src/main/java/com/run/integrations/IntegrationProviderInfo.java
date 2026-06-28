package com.run.integrations;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/20  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 第三方集成供应商信息 }
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class IntegrationProviderInfo {
    /**
     * 供应商标识(对应 Integration.type)
     */
    private String provider;
    /**
     * 供应商名称
     */
    private String name;
}
