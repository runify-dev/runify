package com.run.workflow.deserialize;

import com.run.common.util.ClassScanUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
public class NodeDeserializeManage {

    private static final String BASE_PACKAGE = "com.run.workflow.deserialize";
    private static final ConcurrentHashMap<String, NodeDeserializeManage> CACHE = new ConcurrentHashMap<>();

    private final List<? extends INodeDeserialize> deserializers;

    private NodeDeserializeManage(List<? extends INodeDeserialize> deserializers) {
        this.deserializers = deserializers;
    }

    public static NodeDeserializeManage of() {
        return CACHE.computeIfAbsent(BASE_PACKAGE, key -> {
            List<Class<? extends INodeDeserialize>> classList =
                    ClassScanUtil.getClassList(key, INodeDeserialize.class);
            List<? extends INodeDeserialize> instances = classList.stream()
                    .map(clazz -> {
                        try {
                            return clazz.getDeclaredConstructor().newInstance();
                        } catch (Exception e) {
                            log.error("实例化 INodeDeserialize 失败: {}", clazz.getName(), e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
            return new NodeDeserializeManage(instances);
        });
    }

    /**
     * 根据节点类型查找对应的反序列化器，未找到返回 null
     */
    public INodeDeserialize getDeserialize(String type) {
        for (INodeDeserialize deserializer : deserializers) {
            if (deserializer.support(type)) {
                return deserializer;
            }
        }
        return null;
    }
}
