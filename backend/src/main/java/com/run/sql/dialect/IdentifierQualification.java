package com.run.sql.dialect;

/**
 * Controls which qualifier is used when rendering a fully-qualified table or field name.
 */
public enum IdentifierQualification {
    /** MySQL database name, SQLite attached database name. */
    CATALOG,
    /** PostgreSQL / Oracle / H2 schema name. */
    SCHEMA,
    /** Ignore catalog/schema and render only object name. */
    NONE
}
