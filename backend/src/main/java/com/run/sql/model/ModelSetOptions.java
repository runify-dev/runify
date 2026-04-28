package com.run.sql.model;

public final class ModelSetOptions {
    private final boolean ignoreNulls;
    private final boolean includePrimaryKeys;

    private ModelSetOptions(boolean ignoreNulls, boolean includePrimaryKeys) {
        this.ignoreNulls = ignoreNulls;
        this.includePrimaryKeys = includePrimaryKeys;
    }

    public static ModelSetOptions defaults() {
        return new ModelSetOptions(false, true);
    }

    /**
     * 跳过 null 字段。
     *
     * <p>注意：不能命名为 ignoreNulls()，因为实例 getter 已经叫 ignoreNulls()，
     * Java 不能只通过返回值区分静态方法和实例方法。</p>
     */
    public static ModelSetOptions ignoreNullValues() {
        return new ModelSetOptions(true, true);
    }

    public static ModelSetOptions excludePrimaryKeys() {
        return new ModelSetOptions(false, false);
    }

    public ModelSetOptions withIgnoreNulls(boolean ignoreNulls) {
        return new ModelSetOptions(ignoreNulls, includePrimaryKeys);
    }

    public ModelSetOptions withIncludePrimaryKeys(boolean includePrimaryKeys) {
        return new ModelSetOptions(ignoreNulls, includePrimaryKeys);
    }

    public boolean ignoreNulls() {
        return ignoreNulls;
    }

    public boolean includePrimaryKeys() {
        return includePrimaryKeys;
    }
}
