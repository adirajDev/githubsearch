package com.adidev.githubsearch.service;

import com.adidev.githubsearch.domain.GitHubRepo;
import com.adidev.githubsearch.domain.SearchCriteria;
import com.adidev.githubsearch.github.GitHubSearchClient;
import com.adidev.githubsearch.github.GitHubSearchQuery;
import com.adidev.githubsearch.github.dto.GitHubRepoItem;
import com.adidev.githubsearch.github.dto.GitHubSearchResponse;
import com.adidev.githubsearch.mapper.GitHubRepoMapper;
import com.adidev.githubsearch.repository.GitHubRepoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RepositorySearchService {

    private static final Logger log = LoggerFactory.getLogger(RepositorySearchService.class);

    private final GitHubSearchClient gitHub;
    private final GitHubRepoRepository repository;
    private final GitHubRepoMapper mapper;

    public RepositorySearchService(GitHubSearchClient gitHub, GitHubRepoRepository repository, GitHubRepoMapper mapper) {
        this.gitHub = gitHub;
        this.repository = repository;
        this.mapper = mapper;
    }

    /** Deliberately NOT @Transactional: never hold a DB connection while waiting on a remote API. */
    public List<GitHubRepo> searchAndStore(SearchCriteria criteria) {
        GitHubSearchResponse response = gitHub.search(GitHubSearchQuery.from(criteria));
        if (response.incompleteResults()) {
            log.warn("GitHub returned incomplete results for query '{}'", criteria.query());
        }

        List<GitHubRepoItem> valid = response.items().stream().filter(GitHubRepoMapper::isComplete).toList();
        if (valid.size() < response.items().size()) {
            log.warn("Skipped {} GitHub item(s) with missing required fields", response.items().size() - valid.size());
        }

        List<GitHubRepo> repos = valid.stream().map(mapper::toEntity).toList();
        if (!repos.isEmpty()) {
            repository.upsertAll(repos);
        }
        return repos;
    }
}