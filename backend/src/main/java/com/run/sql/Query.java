package com.run.sql;

import java.util.Map;

public interface Query {
    RenderedSql render();

    default String getSQL() {
        return render().sql();
    }

    default Map<String, Object> getParams() {
        return render().params();
    }
}
