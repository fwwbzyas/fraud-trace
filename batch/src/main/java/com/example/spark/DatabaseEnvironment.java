package com.example.spark;

/** Environment configuration shared by JDBC utilities; secrets never have defaults. */
public final class DatabaseEnvironment {
    private DatabaseEnvironment() {}

    public static String value(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    public static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Set environment variable " + name + " before using this database operation");
        }
        return value;
    }
}
