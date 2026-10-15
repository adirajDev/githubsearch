package com.adidev.githubsearch.repository;

import com.adidev.githubsearch.domain.GitHubRepo;
import java.util.Collection;

public interface GitHubRepoUpsert {
    /** Inserts or updates by github_id. Returns the number of distinct repositories written. */
    int upsertAll(Collection<GitHubRepo> repos);
}