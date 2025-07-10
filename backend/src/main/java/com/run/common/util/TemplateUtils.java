package com.run.common.util;

import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.SneakyThrows;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/21  23:03}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class TemplateUtils {
    private static final Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);

    static {
        cfg.setDefaultEncoding("UTF-8");
    }

    @SneakyThrows
    public static String format(String prompt, Map<String, Object> dataModel) {
        Template template = new Template("memoryTemplate", prompt, cfg);
        Writer out = new StringWriter();
        template.process(dataModel, out);
        return out.toString();

    }

}
