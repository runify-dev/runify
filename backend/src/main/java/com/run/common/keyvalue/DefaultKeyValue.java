package com.run.common.keyvalue;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/24  21:53}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
public class DefaultKeyValue<Key, Value> {
    private Key key;
    private Value value;

    public DefaultKeyValue(Key key, Value value) {
        this.key = key;
        this.value = value;
    }
}
