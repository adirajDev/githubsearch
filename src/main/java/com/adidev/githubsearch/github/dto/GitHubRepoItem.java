package com.adidev.githubsearch.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepoItem(
        long id,
        String name,
        @JsonProperty("full_name") String fullName,
        String description,
        GitHubOwner owner,
        String language,
        @JsonProperty("html_url") String htmlUrl,
        @JsonProperty("stargazers_count") int stargazersCount,
        @JsonProperty("forks_count") int forksCount,
        @JsonProperty("watchers_count") int watchersCount,
        @JsonProperty("open_issues_count") int openIssuesCount,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt,
        @JsonProperty("pushed_at") Instant pushedAt) {}