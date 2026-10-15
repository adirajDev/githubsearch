package com.adidev.githubsearch.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubSearchResponse(
        @JsonProperty("total_count") long totalCount,
        @JsonProperty("incomplete_results") boolean incompleteResults,
        List<GitHubRepoItem> items) {}