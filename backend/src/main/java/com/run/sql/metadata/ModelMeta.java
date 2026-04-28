package com.run.sql.metadata;

import java.util.List;
import java.util.Objects;

public record ModelMeta(TableMeta table, List<ModelColumnMeta> columns) {
    public ModelMeta {
        Objects.requireNonNull(table, "table");
        Objects.requireNonNull(columns, "columns");
    }
}
