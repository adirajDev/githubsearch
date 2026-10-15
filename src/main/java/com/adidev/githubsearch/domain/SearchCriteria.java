package com.adidev.githubsearch.domain;

public record SearchCriteria(String query, String language, SortField sort) {
    public SearchCriteria {
        if (query == null || query.isBlank()) {
            throw new InvalidParameterException("query", "must not be blank");
        }
        if (sort == null) {
            sort = SortField.STARS;
        }
    }
}