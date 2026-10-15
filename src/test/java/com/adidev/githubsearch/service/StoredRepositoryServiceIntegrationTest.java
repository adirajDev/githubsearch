package com.adidev.githubsearch.service;

import com.adidev.githubsearch.TestcontainersConfiguration;
import com.adidev.githubsearch.domain.GitHubRepo;
import com.adidev.githubsearch.domain.RepositoryFilter;
import com.adidev.githubsearch.domain.SortField;
import com.adidev.githubsearch.repository.GitHubRepoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class StoredRepositoryServiceIntegrationTest {

    @Autowired StoredRepositoryService service;
    @Autowired GitHubRepoRepository repository;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE repositories RESTART IDENTITY");
        repository.upsertAll(List.of(
                repo(1, "Java",   100,  5, "2024-01-01T00:00:00Z"),
                repo(2, "Java",   300,  1, "2023-01-01T00:00:00Z"),
                repo(3, "Python", 200, 50, "2025-01-01T00:00:00Z"),
                repo(4, null,     300, 10, "2022-01-01T00:00:00Z")));
    }

    private static GitHubRepo repo(long id, String language, int stars, int forks, String updated) {
        return new GitHubRepo(id, "repo-" + id, "user/repo-" + id, null, "user", language,
                "https://github.com/user/repo-" + id, stars, forks, stars, 0, null, Instant.parse(updated), null);
    }

    private static RepositoryFilter filter(String language, Integer minStars, SortField sort) {
        return new RepositoryFilter(language, minStars, sort, 0, 30);
    }

    private List<Long> ids(RepositoryFilter filter) {
        return service.find(filter).map(GitHubRepo::getGithubId).getContent();
    }

    @Test
    void noFiltersSortsByStarsWithGithubIdTieBreak() {
        assertThat(ids(filter(null, null, null))).containsExactly(2L, 4L, 3L, 1L);
    }

    @Test
    void languageFilterIsCaseInsensitive() {
        assertThat(ids(filter("jAVA", null, null))).containsExactly(2L, 1L);
    }

    @Test
    void minStarsIsInclusive() {
        assertThat(ids(filter(null, 200, null))).containsExactly(2L, 4L, 3L);
    }

    @Test
    void filtersCombine() {
        assertThat(ids(filter("Java", 150, null))).containsExactly(2L);
    }

    @Test
    void sortsByForks() {
        assertThat(ids(filter(null, null, SortField.FORKS))).containsExactly(3L, 4L, 1L, 2L);
    }

    @Test
    void sortsByUpdatedDate() {
        assertThat(ids(filter(null, null, SortField.UPDATED))).containsExactly(3L, 1L, 2L, 4L);
    }

    @Test
    void paginatesAndReportsTotal() {
        var page = service.find(new RepositoryFilter(null, null, null, 1, 2));
        assertThat(page.map(GitHubRepo::getGithubId).getContent()).containsExactly(3L, 1L);
        assertThat(page.getTotalElements()).isEqualTo(4);
    }

    @Test
    void noMatchesReturnsEmptyPage() {
        assertThat(ids(filter("Rust", null, null))).isEmpty();
    }
}