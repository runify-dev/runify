package com.run.integrations.impl.wecomstream;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/28  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 企业微信长连接 出站媒体提取: 从应用回复文本里抽出可作为原生媒体下发的内容并把对应标记从正文剥离。
 * 与个人微信 WeixinPoller.collectMedia/stripAbsMedia 同源:
 * 内部存储引用 ./api/storage/file/{uuid} 直接读字节发原生; 绝对 URL 按后缀分类(图片/视频/文件)。
 * ![](url) 默认图片; [文字](url) 与裸链接只取视频/文件(避免抓网页/重复图片)。 }
 */
public final class WecomStreamOutbound {

    private WecomStreamOutbound() {
    }

    private static final Set<String> IMAGE_EXT = Set.of("png", "jpg", "jpeg", "gif", "webp", "bmp");
    private static final Set<String> VIDEO_EXT = Set.of("mp4", "mov", "webm", "mkv");
    private static final Set<String> FILE_EXT = Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "csv", "txt", "json", "md", "xml", "zip", "rar", "7z", "gz", "tar");
    private static final Pattern IMAGE_MD = Pattern.compile("!\\[[^\\]]*]\\(\\s*(\\S+?)\\s*\\)");
    private static final Pattern LINK_MD = Pattern.compile("(?<!!)\\[[^\\]]*]\\(\\s*(\\S+?)\\s*\\)");
    private static final Pattern VIDEO_TAG = Pattern.compile("(?s)<video[^>]*\\bsrc=[\"']([^\"']+)[\"'][^>]*>(?:.*?</video>)?");
    private static final Pattern BARE_URL = Pattern.compile("(?<![(\\[\"'])https?://[^\\s)\\]]+");
    private static final Pattern STORAGE_REF = Pattern.compile(
            "(?:\\./|/)?api/storage/file/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})");

    /**
     * images/videos/files 为绝对 URL; storageIds 为内部存储文件 id; text 为剥离这些媒体后的正文。
     */
    public record Result(List<String> images, List<String> videos, List<String> files,
                         List<String> storageIds, String text) {
    }

    public static Result collect(String text) {
        List<String> images = new ArrayList<>();
        List<String> videos = new ArrayList<>();
        List<String> files = new ArrayList<>();
        List<String> storageIds = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        Matcher vt = VIDEO_TAG.matcher(text);
        while (vt.find()) {
            add(vt.group(1), "video", images, videos, files, seen);
        }
        Matcher im = IMAGE_MD.matcher(text);
        while (im.find()) {
            String url = im.group(1);
            if (!collectStorage(url, storageIds, seen)) {
                add(url, orElse(classify(url), "image"), images, videos, files, seen);
            }
        }
        Matcher lm = LINK_MD.matcher(text);
        while (lm.find()) {
            String url = lm.group(1);
            if (!collectStorage(url, storageIds, seen)) {
                collectVideoOrFile(url, images, videos, files, seen);
            }
        }
        Matcher bu = BARE_URL.matcher(text);
        while (bu.find()) {
            collectVideoOrFile(bu.group(), images, videos, files, seen);
        }
        Matcher sr = STORAGE_REF.matcher(text);
        while (sr.find()) {
            if (seen.add(sr.group())) {
                storageIds.add(sr.group(1));
            }
        }
        return new Result(images, videos, files, storageIds, strip(text));
    }

    private static boolean collectStorage(String url, List<String> storageIds, Set<String> seen) {
        Matcher m = STORAGE_REF.matcher(url);
        if (m.find()) {
            if (seen.add(url)) {
                storageIds.add(m.group(1));
            }
            return true;
        }
        return false;
    }

    private static void collectVideoOrFile(String url, List<String> images, List<String> videos, List<String> files, Set<String> seen) {
        String k = classify(url);
        if ("video".equals(k) || "file".equals(k)) {
            add(url, k, images, videos, files, seen);
        }
    }

    private static void add(String url, String kind, List<String> images, List<String> videos, List<String> files, Set<String> seen) {
        if (!isAbs(url) || !seen.add(url)) {
            return;
        }
        switch (kind) {
            case "video" -> videos.add(url);
            case "file" -> files.add(url);
            default -> images.add(url);
        }
    }

    /**
     * 去掉正文里已作为原生媒体发送的标记(绝对 URL 媒体 + 内部存储引用); 普通网页链接保留
     */
    private static String strip(String text) {
        text = VIDEO_TAG.matcher(text).replaceAll(mr -> isAbs(mr.group(1)) ? "" : Matcher.quoteReplacement(mr.group(0)));
        text = IMAGE_MD.matcher(text).replaceAll(mr -> (isAbs(mr.group(1)) || isStorageRef(mr.group(1))) ? "" : Matcher.quoteReplacement(mr.group(0)));
        text = LINK_MD.matcher(text).replaceAll(mr -> (isSentLink(mr.group(1)) || isStorageRef(mr.group(1))) ? "" : Matcher.quoteReplacement(mr.group(0)));
        text = BARE_URL.matcher(text).replaceAll(mr -> isSentLink(mr.group()) ? "" : Matcher.quoteReplacement(mr.group()));
        text = STORAGE_REF.matcher(text).replaceAll("");
        return text.replaceAll("\\n{3,}", "\n\n").strip();
    }

    private static boolean isStorageRef(String url) {
        return url != null && STORAGE_REF.matcher(url).find();
    }

    private static boolean isSentLink(String url) {
        String k = classify(url);
        return isAbs(url) && ("video".equals(k) || "file".equals(k));
    }

    /**
     * 按 URL/文件名后缀判类型: image/video/file/null
     */
    public static String classify(String url) {
        String e = ext(url);
        if (IMAGE_EXT.contains(e)) {
            return "image";
        }
        if (VIDEO_EXT.contains(e)) {
            return "video";
        }
        if (FILE_EXT.contains(e)) {
            return "file";
        }
        return null;
    }

    private static String ext(String url) {
        String path = url.split("[?#]")[0].toLowerCase();
        int dot = path.lastIndexOf('.');
        return dot < 0 ? "" : path.substring(dot + 1);
    }

    public static String fileNameFromUrl(String url, String kind) {
        String path = url.split("[?#]")[0];
        int idx = path.lastIndexOf('/');
        String name = idx >= 0 ? path.substring(idx + 1) : path;
        if (name.isEmpty() || !name.contains(".")) {
            name = switch (kind) {
                case "video" -> "video.mp4";
                case "file" -> "file.bin";
                default -> "image.jpg";
            };
        }
        return name;
    }

    private static String orElse(String s, String fallback) {
        return s == null ? fallback : s;
    }

    private static boolean isAbs(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }
}
