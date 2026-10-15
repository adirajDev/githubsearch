package com.adidev.githubsearch.repository;

import com.adidev.githubsearch.TestcontainersConfiguration;
import com.adidev.githubsearch.domain.GitHubRepo;
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
class GitHubRepoUpsertIntegrationTest {

    @Autowired GitHubRepoRepository repository;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE repositories RESTART IDENTITY");
    }

    private static GitHubRepo repo(long githubId, int stars, String description) {
        return new GitHubRepo(githubId, "repo-" + githubId, "user/repo-" + githubId, description,
                "user", "Java", "https://github.com/user/repo-" + githubId,
                stars, 5, stars, 1,
                Instant.parse("2020-01-01T00:00:00Z"), Instant.parse("2024-01-01T12:00:00Z"), null);
    }

    @Test
    void insertsNewRepositories() {
        int processed = repository.upsertAll(List.of(repo(1, 10, "a"), repo(2, 20, "b")));

        assertThat(processed).isEqualTo(2);
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void sameGithubIdTwiceUpdatesInsteadOfDuplicating() {
        repository.upsertAll(List.of(repo(1, 10, "old")));
        Long internalIdBefore = repository.findByGithubId(1L).orElseThrow().getId();

        repository.upsertAll(List.of(repo(1, 99, "new")));

        var saved = repository.findByGithubId(1L).orElseThrow();
        assertThat(repository.count()).isEqualTo(1);
        assertThat(saved.getStars()).isEqualTo(99);
        assertThat(saved.getDescription()).isEqualTo("new");
        assertThat(saved.getId()).isEqualTo(internalIdBefore);
    }

    @Test
    void storesNullDescriptionAndLanguage() {
        var noLang = new GitHubRepo(3L, "x", "user/x", null, "user", null, "https://github.com/user/x",
                0, 0, 0, 0, null, Instant.parse("2024-01-01T12:00:00Z"), null);

        repository.upsertAll(List.of(noLang));

        var saved = repository.findByGithubId(3L).orElseThrow();
        assertThat(saved.getDescription()).isNull();
        assertThat(saved.getLanguage()).isNull();
    }

    @Test
    void duplicateIdsInSameBatchKeepLastOccurrence() {
        int processed = repository.upsertAll(List.of(repo(1, 10, "first"), repo(1, 50, "last")));

        assertThat(processed).isEqualTo(1);
        assertThat(repository.findByGithubId(1L).orElseThrow().getStars()).isEqualTo(50);
    }

    @Test
    void emptyInputIsANoOp() {
        assertThat(repository.upsertAll(List.of())).isZero();
        assertThat(repository.count()).isZero();
    }
}