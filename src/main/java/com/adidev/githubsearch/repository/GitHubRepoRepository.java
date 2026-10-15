package com.adidev.githubsearch.repository;

import com.adidev.githubsearch.domain.GitHubRepo;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface GitHubRepoRepository
        extends Repository<GitHubRepo, Long>, JpaSpecificationExecutor<GitHubRepo> {

    Optional<GitHubRepo> findByGithubId(Long githubId);

    long count();
}