package com.run.common.util;


import org.commonmark.node.Code;
import org.commonmark.node.Heading;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.markdown.MarkdownRenderer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

public class MarkdownChunker {

    private static final String TITLE_SEPARATOR = " » ";

    /**
     * 一个最终 chunk。
     *
     * @param id           稳定 ID，可用于入库
     * @param titlePath    标题路径，例如：MaxKB v1到v2迁移工具 » 迁移操作步骤 » Linux/macOS 系统
     * @param headingLevel 当前章节标题等级
     * @param partIndex    同一个章节下的第几个 chunk，从 1 开始
     * @param content      实际用于索引/向量化的内容
     */
    public record MarkdownChunk(
            String id,
            String titlePath,
            int headingLevel,
            int partIndex,
            String content
    ) {
    }

    private record HeadingInfo(int level, String title) {
    }

    private static class Section {
        private final String titlePath;
        private final int headingLevel;
        private final List<String> blocks = new ArrayList<>();

        private Section(String titlePath, int headingLevel) {
            this.titlePath = titlePath;
            this.headingLevel = headingLevel;
        }

        private boolean hasContent() {
            return blocks.stream().anyMatch(s -> s != null && !s.isBlank());
        }
    }

    /**
     * 兼容你原来的 Map 返回值。
     */
    public static Map<String, String> splitToMapWithParents(String markdown) {
        List<MarkdownChunk> chunks = splitToChunks(markdown, 1200, 150);

        Map<String, String> map = new LinkedHashMap<>();
        for (MarkdownChunk chunk : chunks) {
            String key = chunk.titlePath().isBlank() ? "正文" : chunk.titlePath();

            if (chunk.partIndex() > 1) {
                key = key + " #" + chunk.partIndex();
            }

            key = uniqueKey(map, key);
            map.put(key, chunk.content());
        }
        return map;
    }

    /**
     * 推荐直接用这个。
     *
     * @param markdown     原始 Markdown
     * @param maxChars     每个 chunk 最大字符数，中文知识库建议 800~1500
     * @param overlapChars 相邻 chunk 重叠字符数，建议 80~200
     */
    public static List<MarkdownChunk> splitToChunks(String markdown, int maxChars, int overlapChars) {
        if (markdown == null || markdown.isBlank()) {
            return List.of();
        }
        if (maxChars < 200) {
            throw new IllegalArgumentException("maxChars 不建议小于 200");
        }
        if (overlapChars < 0 || overlapChars >= maxChars) {
            throw new IllegalArgumentException("overlapChars 必须 >= 0 且小于 maxChars");
        }

        List<Section> sections = parseSections(markdown);
        List<MarkdownChunk> chunks = new ArrayList<>();

        for (Section section : sections) {
            String prefix = section.titlePath.isBlank()
                    ? ""
                    : "标题路径：" + section.titlePath + "\n\n";

            int bodyMaxChars = Math.max(200, maxChars - prefix.length());
            List<String> parts = splitBlocks(section.blocks, bodyMaxChars, overlapChars);

            int partIndex = 1;
            for (String part : parts) {
                String content = (prefix + part).trim();
                if (content.isBlank()) {
                    continue;
                }

                String id = stableId(section.titlePath + ":" + partIndex + ":" + content);

                chunks.add(new MarkdownChunk(
                        id,
                        section.titlePath,
                        section.headingLevel,
                        partIndex,
                        content
                ));

                partIndex++;
            }
        }

        return chunks;
    }

    private static List<Section> parseSections(String markdown) {
        Parser parser = Parser.builder().build();
        MarkdownRenderer renderer = MarkdownRenderer.builder().build();

        Node document = parser.parse(markdown);

        List<Section> sections = new ArrayList<>();
        Deque<HeadingInfo> headingStack = new ArrayDeque<>();

        Section current = new Section("", 0);

        for (Node node = document.getFirstChild(); node != null; node = node.getNext()) {
            if (node instanceof Heading heading) {
                addSectionIfNotEmpty(sections, current);

                String headingText = plainText(heading).trim();

                while (!headingStack.isEmpty()
                        && headingStack.peekLast().level() >= heading.getLevel()) {
                    headingStack.pollLast();
                }

                headingStack.offerLast(new HeadingInfo(heading.getLevel(), headingText));

                current = new Section(buildTitlePath(headingStack), heading.getLevel());
            } else {
                String block = renderer.render(node).trim();
                if (!block.isBlank()) {
                    current.blocks.add(block);
                }
            }
        }

        addSectionIfNotEmpty(sections, current);
        return sections;
    }

    private static void addSectionIfNotEmpty(List<Section> sections, Section section) {
        if (section != null && section.hasContent()) {
            sections.add(section);
        }
    }

    private static String buildTitlePath(Deque<HeadingInfo> headingStack) {
        return headingStack.stream()
                .map(HeadingInfo::title)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(TITLE_SEPARATOR));
    }

    private static List<String> splitBlocks(List<String> blocks, int maxChars, int overlapChars) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String rawBlock : blocks) {
            String block = rawBlock == null ? "" : rawBlock.trim();
            if (block.isBlank()) {
                continue;
            }

            // 单个块太长，单独处理，避免把前面的正常段落也拖进去。
            if (block.length() > maxChars) {
                flush(result, current);

                if (isCodeBlock(block)) {
                    // 代码块尽量不拆，防止语义和格式被破坏。
                    result.add(block);
                } else {
                    result.addAll(splitLongText(block, maxChars, overlapChars));
                }

                continue;
            }

            int nextLength = current.length() == 0
                    ? block.length()
                    : current.length() + 2 + block.length();

            if (nextLength > maxChars && current.length() > 0) {
                String flushed = current.toString().trim();
                result.add(flushed);

                current.setLength(0);

                String overlap = tailOverlap(flushed, overlapChars);
                if (!overlap.isBlank()) {
                    current.append(overlap);
                }

                // 如果加了 overlap 后放不下当前 block，就舍弃 overlap。
                if (current.length() > 0
                        && current.length() + 2 + block.length() > maxChars) {
                    current.setLength(0);
                }
            }

            if (current.length() > 0) {
                current.append("\n\n");
            }
            current.append(block);
        }

        flush(result, current);
        return result;
    }

    private static void flush(List<String> result, StringBuilder current) {
        if (current.length() == 0) {
            return;
        }

        String text = current.toString().trim();
        if (!text.isBlank()) {
            result.add(text);
        }

        current.setLength(0);
    }

    private static List<String> splitLongText(String text, int maxChars, int overlapChars) {
        List<String> result = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int hardEnd = Math.min(start + maxChars, text.length());
            int end = hardEnd;

            if (hardEnd < text.length()) {
                end = findBestCut(text, start, hardEnd);
            }

            if (end <= start) {
                end = hardEnd;
            }

            String part = text.substring(start, end).trim();
            if (!part.isBlank()) {
                result.add(part);
            }

            if (end >= text.length()) {
                break;
            }

            start = Math.max(end - overlapChars, start + 1);
        }

        return result;
    }

    private static int findBestCut(String text, int start, int hardEnd) {
        int min = start + Math.max(1, (int) ((hardEnd - start) * 0.6));
        String range = text.substring(min, hardEnd);

        String[] separators = {
                "\n\n",
                "\n",
                "。",
                "！",
                "？",
                "；",
                ". ",
                "! ",
                "? ",
                "; ",
                "，",
                ", "
        };

        for (String separator : separators) {
            int index = range.lastIndexOf(separator);
            if (index >= 0) {
                return min + index + separator.length();
            }
        }

        return hardEnd;
    }

    private static String tailOverlap(String text, int overlapChars) {
        if (overlapChars <= 0 || text == null || text.isBlank()) {
            return "";
        }

        String value = text.trim();
        if (value.length() <= overlapChars) {
            return value;
        }

        int start = value.length() - overlapChars;

        // 尽量从换行后开始，避免截在半句话中间。
        int lineBreak = value.indexOf('\n', start);
        if (lineBreak >= 0 && lineBreak < value.length() - 20) {
            return value.substring(lineBreak + 1).trim();
        }

        return value.substring(start).trim();
    }

    private static boolean isCodeBlock(String block) {
        String value = block.stripLeading();
        return value.startsWith("```") || value.startsWith("    ");
    }

    private static String plainText(Node node) {
        StringBuilder sb = new StringBuilder();
        appendPlainText(node, sb);
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private static void appendPlainText(Node node, StringBuilder sb) {
        if (node instanceof Text text) {
            sb.append(text.getLiteral());
            return;
        }

        if (node instanceof Code code) {
            sb.append(code.getLiteral());
            return;
        }

        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            appendPlainText(child, sb);
        }
    }

    private static String uniqueKey(Map<String, String> map, String baseKey) {
        if (!map.containsKey(baseKey)) {
            return baseKey;
        }

        int index = 2;
        String key;
        do {
            key = baseKey + " (" + index + ")";
            index++;
        } while (map.containsKey(key));

        return key;
    }

    private static String stableId(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", bytes[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString();
        }
    }
}