package com.run.common.util;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/6  17:24}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ValidatorUtil {
    private final static Validator validator;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    public static <T> void validate(T var1, Class<?>... var2) {
        Set<ConstraintViolation<T>> validate = validator.validate(var1, var2);
        if (validate.size() > 0) {
            ConstraintViolation<T> next = validate.iterator().next();
            String message = next.getMessage();
            throw new RuntimeException(message);
        }
    }


}
