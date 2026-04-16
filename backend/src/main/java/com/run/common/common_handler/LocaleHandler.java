package com.run.common.common_handler;

import io.vertx.ext.web.RoutingContext;

import java.util.Locale;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/16  21:57}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class LocaleHandler {

    public static void handle(RoutingContext ctx) {
        Locale locale = resolveLocale(ctx);
        ctx.put("locale", locale);
        ctx.next();
    }

    public static Locale resolveLocale(RoutingContext ctx) {
        String lang = ctx.request().getParam("lang");
        if (lang != null && !lang.isBlank()) {
            return toLocale(lang);
        }

        String header = ctx.request().getHeader("Accept-Language");
        if (header != null && !header.isBlank()) {
            String lower = header.toLowerCase();

            if (lower.contains("zh-tw") || lower.contains("zh-hk") || lower.contains("zh-hant")) {
                return Locale.TRADITIONAL_CHINESE; // zh_TW
            }
            if (lower.contains("zh-cn") || lower.contains("zh-sg") || lower.contains("zh-hans") || lower.contains("zh")) {
                return Locale.SIMPLIFIED_CHINESE; // zh_CN
            }
            if (lower.contains("en")) {
                return Locale.US; // en_US
            }
        }

        return Locale.SIMPLIFIED_CHINESE;
    }

    private static Locale toLocale(String lang) {
        return switch (lang) {
            case "zh", "zh-CN", "zh-SG", "zh-Hans" -> Locale.SIMPLIFIED_CHINESE;
            case "zh-TW", "zh-HK", "zh-Hant" -> Locale.TRADITIONAL_CHINESE;
            case "en", "en-US" -> Locale.US;
            default -> Locale.SIMPLIFIED_CHINESE;
        };
    }
}