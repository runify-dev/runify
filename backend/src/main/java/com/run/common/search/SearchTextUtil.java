package com.run.common.search;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class SearchTextUtil {

    private SearchTextUtil() {
    }

    public static String normalizeCode(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toLowerCase()
                .replace("-", "")
                .replace("_", "")
                .replace(" ", "");
    }

    public static boolean isNumericCollection(Object value) {
        if (!(value instanceof Collection<?> collection) || collection.isEmpty()) {
            return false;
        }
        for (Object item : collection) {
            if (!(item instanceof Number)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isFloatVectorArray(Object value) {
        return value instanceof float[] || value instanceof double[];
    }

    public static boolean isNumericArray(Object value) {
        if (value == null || !value.getClass().isArray()) {
            return false;
        }
        Class<?> componentType = value.getClass().getComponentType();
        return componentType != null && Number.class.isAssignableFrom(componentType);
    }

    public static float[] toFloatVector(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof float[] floats) {
            return floats.clone();
        }
        if (value instanceof double[] doubles) {
            float[] result = new float[doubles.length];
            for (int i = 0; i < doubles.length; i++) {
                result[i] = (float) doubles[i];
            }
            return result;
        }
        if (value instanceof Collection<?> collection) {
            List<Float> values = new ArrayList<>();
            for (Object item : collection) {
                if (!(item instanceof Number number)) {
                    throw new IllegalArgumentException("vector collection must only contain numbers");
                }
                values.add(number.floatValue());
            }
            float[] result = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                result[i] = values.get(i);
            }
            return result;
        }
        if (isNumericArray(value)) {
            int length = Array.getLength(value);
            float[] result = new float[length];
            for (int i = 0; i < length; i++) {
                Object item = Array.get(value, i);
                if (!(item instanceof Number number)) {
                    throw new IllegalArgumentException("vector array must only contain numbers");
                }
                result[i] = number.floatValue();
            }
            return result;
        }
        throw new IllegalArgumentException("unsupported vector value type: " + value.getClass());
    }
}
