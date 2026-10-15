package com.adidev.githubsearch.domain;

public enum SortField {
    STARS("stars"), FORKS("forks"), UPDATED("updated");

    private final String value;

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
        throw new IllegalArgumentException(
                "Invalid sort '" + raw + "'. Allowed values: stars, forks, updated");
    }
}