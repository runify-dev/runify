package com.run.integrations.wecom;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/20  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 企业微信回调 XML 字段提取(应用/机器人共用) }
 */
public class WecomXml {

    public static String extract(String xml, String tag) {
        if (xml == null) {
            return null;
        }
        Matcher m = Pattern.compile("<" + tag + ">(?:<!\\[CDATA\\[)?(.*?)(?:\\]\\]>)?</" + tag + ">", Pattern.DOTALL).matcher(xml);
        return m.find() ? m.group(1) : null;
    }

    public static String orElse(String s, String fallback) {
        return (s == null || s.isEmpty()) ? fallback : s;
    }
}
