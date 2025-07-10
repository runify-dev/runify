package com.run.common.forms.label;

import com.run.common.forms.BaseLabel;

import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/25  21:45}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class TextLabel extends BaseLabel {
    public TextLabel(String value, Map<String, Object> attrs, Map<String, Object> props) {
        super("Text", value, attrs, props);
    }

    public TextLabel(String value) {
        super("Text", value, null, null);
    }
}
