package com.adidev.githubsearch.domain;

public enum SortField {
    STARS("stars"), FORKS("forks"), UPDATED("updated");

    private final String value;
    public static final String PATTERN = "(?i)stars|forks|updated";
    public static final String ALLOWED_MESSAGE = "must be one of: stars, forks, updated";

    SortField(String value) { this.value = value; }

    public String value() { return value; }

    public static SortField fromValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return STARS;
        }
        for (SortField field : values()) {
            if (field.value.equalsIgnoreCase(raw.trim())) {
                return field;
            }
        }
        // in fromValue(), replace the throw with:
        throw new InvalidParameterException("sort", ALLOWED_MESSAGE);
    }
}