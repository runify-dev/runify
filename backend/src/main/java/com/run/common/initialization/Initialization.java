package com.run.common.initialization;

import com.google.inject.Injector;
import com.run.common.util.ClassScanUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/15  22:20}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface Initialization {
    int getOrder();

    Injector initialization(Injector injector);

    static Injector init(Injector injector) {
        List<Class<? extends Initialization>> classList = ClassScanUtil.getClassList("com.run.common.initialization", Initialization.class);
        List<? extends Initialization> initializations = classList.stream().map((aClass) -> {
            try {
                return aClass.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                return null;
            }
        }).filter(Objects::nonNull).sorted(Comparator.comparing(Initialization::getOrder)).toList();
        for (Initialization initialization : initializations) {
            injector = initialization.initialization(injector);
        }
        return injector;
    }
}
