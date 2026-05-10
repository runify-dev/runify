package com.run.datasources;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 数据源供应商信息
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DatasourceProviderInfo {
    /**
     * 供应商标识
     */
    private String provider;
    /**
     * 供应商名称
     */
    private String name;
    /**
     * 图标 SVG
     */
    private String icon;
    /**
     * 数据源类型
     */
    private DataSourceType type;
}
