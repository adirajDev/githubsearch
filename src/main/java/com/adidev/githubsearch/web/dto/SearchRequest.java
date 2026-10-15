package com.adidev.githubsearch.web.dto;

import com.adidev.githubsearch.domain.SearchCriteria;
import com.adidev.githubsearch.domain.SortField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Criteria for searching GitHub repositories by name")
public record SearchRequest(

        @Schema(description = "Text matched against repository names (GitHub `in:name` qualifier)",
                example = "spring boot")
        @NotBlank @Size(max = 200)
        String query,

        @Schema(description = "Programming language filter, as GitHub names it. Omit to search all languages.",
                example = "Java")
        @Size(max = 50)
        String language,

        @Schema(description = "Sort order, always descending.",
                allowableValues = {"stars", "forks", "updated"},
                example = "stars", defaultValue = "stars")
        @Pattern(regexp = SortField.PATTERN, message = SortField.ALLOWED_MESSAGE)
        String sort) {

    public SearchCriteria toCriteria() {

        return new SearchCriteria(query, language, SortField.fromValue(sort));
    }
}
