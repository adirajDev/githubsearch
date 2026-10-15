package com.adidev.githubsearch.service;

import com.adidev.githubsearch.domain.GitHubRepo;
import com.adidev.githubsearch.domain.SearchCriteria;
import com.adidev.githubsearch.domain.SortField;
import com.adidev.githubsearch.github.GitHubSearchClient;
import com.adidev.githubsearch.github.GitHubSearchQuery;
import com.adidev.githubsearch.github.dto.GitHubOwner;
import com.adidev.githubsearch.github.dto.GitHubRepoItem;
import com.adidev.githubsearch.github.dto.GitHubSearchResponse;
import com.adidev.githubsearch.github.exception.GitHubRateLimitException;
import com.adidev.githubsearch.mapper.GitHubRepoMapper;
import com.adidev.githubsearch.repository.GitHubRepoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RepositorySearchServiceTest {

    private final GitHubRepoRepository repository = mock(GitHubRepoRepository.class);
    private final GitHubRepoMapper mapper = new GitHubRepoMapper();
    private final AtomicReference<GitHubSearchQuery> sentQuery = new AtomicReference<>();

    private static GitHubRepoItem item(long id, GitHubOwner owner) {
        return new GitHubRepoItem(id, "repo-" + id, "user/repo-" + id, null, owner, "Java",
                "https://github.com/user/repo-" + id, 10, 2, 10, 0,
                null, Instant.parse("2024-01-01T12:00:00Z"), null);
    }

    private RepositorySearchService serviceReturning(List<GitHubRepoItem> items) {
        GitHubSearchClient fakeGitHub = query -> {
            sentQuery.set(query);
            return new GitHubSearchResponse(items.size(), false, items);
        };
        return new RepositorySearchService(fakeGitHub, repository, mapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchesGitHubAndUpsertsResults() {
        var service = serviceReturning(List.of(item(1, new GitHubOwner("user")), item(2, new GitHubOwner("user"))));

        List<GitHubRepo> result = service.searchAndStore(new SearchCriteria("spring", "Java", SortField.FORKS));

        assertThat(sentQuery.get().q()).isEqualTo("spring in:name language:Java");
        assertThat(sentQuery.get().sort()).isEqualTo("forks");
        assertThat(result).extracting(GitHubRepo::getGithubId).containsExactly(1L, 2L);

        ArgumentCaptor<Collection<GitHubRepo>> saved = ArgumentCaptor.forClass(Collection.class);
        verify(repository).upsertAll(saved.capture());
        assertThat(saved.getValue()).hasSize(2);
    }

    @Test
    void emptyResultsReturnEmptyListAndSkipDatabase() {
        var service = serviceReturning(List.of());

        assertThat(service.searchAndStore(new SearchCriteria("zzzz-no-match", null, null))).isEmpty();
        verify(repository, never()).upsertAll(any());
    }

    @Test
    void incompleteItemsAreSkipped() {
        var service = serviceReturning(List.of(item(1, new GitHubOwner("user")), item(2, null)));

        assertThat(service.searchAndStore(new SearchCriteria("spring", null, null)))
                .extracting(GitHubRepo::getGithubId).containsExactly(1L);
    }

    @Test
    void gitHubFailureStoresNothing() {
        GitHubSearchClient failing = query -> { throw new GitHubRateLimitException(60); };
        var service = new RepositorySearchService(failing, repository, mapper);

        assertThatThrownBy(() -> service.searchAndStore(new SearchCriteria("spring", null, null)))
                .isInstanceOf(GitHubRateLimitException.class);
        verifyNoInteractions(repository);
    }
}