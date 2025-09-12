package com.run.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2024/1/27  12:03}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Page<T> {
    protected List<T> records;
    protected long total;
    protected long current;
    protected long size;
}
