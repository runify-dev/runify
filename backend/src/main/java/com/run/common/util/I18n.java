package com.run.common.util;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/16  21:53}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class I18n {

    private static final String BUNDLE_NAME = "i18n/messages";

    public static String get(String key, Locale locale, Object... args) {
        if (locale == null) {
            locale = Locale.SIMPLIFIED_CHINESE;
        }

        ResourceBundle bundle = ResourceBundle.getBundle(
                BUNDLE_NAME,
                locale,
                new UTF8Control()
        );

        String value = bundle.containsKey(key) ? bundle.getString(key) : key;
        return args != null && args.length > 0
                ? MessageFormat.format(value, args)
                : value;
    }

    public static class UTF8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                        ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {

            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");

            try (InputStream stream = loader.getResourceAsStream(resourceName)) {
                if (stream == null) {
                    return null;
                }
                return new PropertyResourceBundle(
                        new InputStreamReader(stream, StandardCharsets.UTF_8)
                );
            }
        }
    }
}