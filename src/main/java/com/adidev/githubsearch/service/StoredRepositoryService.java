package com.adidev.githubsearch.service;

import com.adidev.githubsearch.domain.GitHubRepo;
import com.adidev.githubsearch.domain.RepositoryFilter;
import com.adidev.githubsearch.domain.SortField;
import com.adidev.githubsearch.repository.GitHubRepoRepository;
import com.adidev.githubsearch.repository.GitHubRepoSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StoredRepositoryService {

    private final GitHubRepoRepository repository;

    public StoredRepositoryService(GitHubRepoRepository repository) {
        this.repository = repository;
    }

    public Page<GitHubRepo> find(RepositoryFilter filter) {
        Sort sort = Sort.by(Sort.Direction.DESC, sortProperty(filter.sort()))
                .and(Sort.by(Sort.Direction.ASC, "githubId"));
        return repository.findAll(
                GitHubRepoSpecifications.matching(filter),
                PageRequest.of(filter.page(), filter.size(), sort));
    }

    static String sortProperty(SortField field) {
        return switch (field) {
            case STARS -> "stars";
            case FORKS -> "forks";
            case UPDATED -> "lastUpdated";
        };
    }
}