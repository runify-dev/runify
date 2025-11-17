package com.run.dao.common.convert;


/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/15  15:40}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface Converter<F, T> extends Deserialize<F>, Serialize<T> {

  
}
