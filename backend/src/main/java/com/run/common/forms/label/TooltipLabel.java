package com.run.common.forms.label;

import com.run.common.forms.BaseLabel;

import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/8/7  21:41}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class TooltipLabel extends BaseLabel {
    public TooltipLabel(String value, Map<String, Object> attrs, Map<String, Object> props) {
        super("TooltipLabel", value, attrs, props);
    }

    public TooltipLabel(String value) {
        super("TooltipLabel", value, null, null);
    }
}
