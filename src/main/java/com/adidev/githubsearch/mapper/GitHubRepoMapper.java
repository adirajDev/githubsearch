package com.adidev.githubsearch.mapper;

import com.adidev.githubsearch.domain.GitHubRepo;
import com.adidev.githubsearch.github.dto.GitHubRepoItem;
import com.adidev.githubsearch.web.dto.RepositoryResponse;
import org.springframework.stereotype.Component;

@Component
public class GitHubRepoMapper {

    /** Guards the NOT NULL columns: an item that fails this is an invalid API response. */
    public static boolean isComplete(GitHubRepoItem item) {
        return item != null
                && item.id() > 0
                && item.name() != null
                && item.fullName() != null
                && item.htmlUrl() != null
                && item.owner() != null && item.owner().login() != null
                && item.updatedAt() != null;
    }

    public GitHubRepo toEntity(GitHubRepoItem item) {
        return new GitHubRepo(
                item.id(), item.name(), item.fullName(), item.description(),
                item.owner().login(), item.language(), item.htmlUrl(),
                item.stargazersCount(), item.forksCount(), item.watchersCount(), item.openIssuesCount(),
                item.createdAt(), item.updatedAt(), item.pushedAt());
    }

    public RepositoryResponse toResponse(GitHubRepo repo) {
        return new RepositoryResponse(
                repo.getGithubId(), repo.getName(), repo.getDescription(), repo.getOwnerLogin(),
                repo.getLanguage(), repo.getStars(), repo.getForks(), repo.getLastUpdated());
    }
}