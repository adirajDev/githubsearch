package com.adidev.githubsearch.github;

import com.adidev.githubsearch.github.dto.GitHubSearchResponse;

/** Port: the rest of the app depends on this, never on HTTP. */
@FunctionalInterface
public interface GitHubSearchClient {
    GitHubSearchResponse search(GitHubSearchQuery query);
}