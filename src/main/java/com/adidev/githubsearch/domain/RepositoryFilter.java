package com.adidev.githubsearch.domain;

public record RepositoryFilter(String language, Integer minStars, SortField sort, int page, int size) {

    public static final int DEFAULT_SIZE = 30;
    public static final int MAX_SIZE = 100;

    public RepositoryFilter {
        if (minStars != null && minStars < 0) {
            throw new InvalidParameterException("minStars", "must be greater than or equal to 0");
        }
        if (page < 0) {
            throw new InvalidParameterException("page", "must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidParameterException("size", "must be between 1 and " + MAX_SIZE);
        }
        language = (language == null || language.isBlank()) ? null : language.trim();
        if (sort == null) {
            sort = SortField.STARS;
        }
    }
}