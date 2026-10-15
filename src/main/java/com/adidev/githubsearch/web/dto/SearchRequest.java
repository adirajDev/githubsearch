package com.adidev.githubsearch.web.dto;

import com.adidev.githubsearch.domain.SearchCriteria;
import com.adidev.githubsearch.domain.SortField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SearchRequest(
        @NotBlank @Size(max = 200) String query,
        @Size(max = 50) String language,
        @Pattern(regexp = SortField.PATTERN, message = SortField.ALLOWED_MESSAGE) String sort) {

    public SearchCriteria toCriteria() {
        return new SearchCriteria(query, language, SortField.fromValue(sort));
    }
}