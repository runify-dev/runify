package com.run.common.util;

import org.jooq.*;
import org.jooq.impl.DefaultVisitListener;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/9/3  00:15}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class MyRenderContext implements VisitListener {
    @Override
    public void visitStart(VisitContext context) {
        if (context.renderContext() != null && context.queryPart() instanceof Param<?>) {
            Param<?> param = (Param<?>) context.queryPart();
            if (param.getParamName() != null) {
                RenderContext renderContext = context.renderContext();
                renderContext.declareFields();
            }
        }
    }
}
