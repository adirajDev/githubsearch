package com.adidev.githubsearch.github;

import com.adidev.githubsearch.domain.SearchCriteria;

public record GitHubSearchQuery(String q, String sort, String order, int perPage) {

    public static final int DEFAULT_PER_PAGE = 30;

    public static GitHubSearchQuery from(SearchCriteria criteria) {
        StringBuilder q = new StringBuilder(criteria.query().trim()).append(" in:name");
        String language = criteria.language();
        if (language != null && !language.isBlank()) {
            q.append(" language:").append(qualifierValue(language.trim()));
        }
        return new GitHubSearchQuery(q.toString(), criteria.sort().value(), "desc", DEFAULT_PER_PAGE);
    }

    private static String qualifierValue(String value) {
        return value.chars().anyMatch(Character::isWhitespace) ? "\"" + value + "\"" : value;
    }
}