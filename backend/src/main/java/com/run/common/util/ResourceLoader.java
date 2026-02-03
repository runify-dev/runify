package com.run.common.util;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/1/5  00:51}
 * {@code @Version 1.0}
 * {@code @注释: }
 */

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class ResourceLoader {


    @SneakyThrows
    public static List<String> getAdminResources() {
        List<String> resources = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // 获取 admin 目录的 URL
        Enumeration<URL> urls = classLoader.getResources("admin");

        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();

            if (url.getProtocol().equals("jar")) {
                // JAR 包
                JarURLConnection jarConn = (JarURLConnection) url.openConnection();
                try (JarFile jar = jarConn.getJarFile()) {
                    jar.stream()
                            .filter(entry -> !entry.isDirectory())
                            .map(JarEntry::getName)
                            .filter(name -> name.startsWith("admin/"))
                            .map(name -> name.substring("admin/".length()))
                            .forEach(resources::add);
                }
            } else {
                // 文件系统
                try {
                    java.nio.file.Path dir = java.nio.file.Paths.get(url.toURI());
                    java.util.stream.Stream<java.nio.file.Path> stream =
                            java.nio.file.Files.walk(dir);
                    stream.filter(java.nio.file.Files::isRegularFile)
                            .map(dir::relativize)
                            .map(path -> path.toString().replace("\\", "/"))
                            .forEach(resources::add);
                    stream.close();
                } catch (Exception e) {
                    // 忽略文件系统访问错误
                }
            }
        }

        // 如果什么都没找到，尝试直接读取 JAR 包
        if (resources.isEmpty()) {
            URL classpathRoot = classLoader.getResource("");
            if (classpathRoot != null && classpathRoot.getProtocol().equals("jar")) {
                JarURLConnection conn = (JarURLConnection) classpathRoot.openConnection();
                try (JarFile jar = conn.getJarFile()) {
                    jar.stream()
                            .filter(entry -> !entry.isDirectory())
                            .map(JarEntry::getName)
                            .filter(name -> name.startsWith("admin/"))
                            .map(name -> name.substring("admin/".length()))
                            .forEach(resources::add);
                }
            }
        }

        return resources.stream()
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

}