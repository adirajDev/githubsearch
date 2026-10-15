package com.adidev.githubsearch.mapper;

import com.adidev.githubsearch.github.dto.GitHubOwner;
import com.adidev.githubsearch.github.dto.GitHubRepoItem;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubRepoMapperTest {

    private final GitHubRepoMapper mapper = new GitHubRepoMapper();
    private static final Instant UPDATED = Instant.parse("2024-01-01T12:00:00Z");

    private static GitHubRepoItem item(String description, String language, GitHubOwner owner, Instant updatedAt) {
        return new GitHubRepoItem(123456L, "spring-boot-example", "user123/spring-boot-example",
                description, owner, language, "https://github.com/user123/spring-boot-example",
                450, 120, 450, 3, Instant.parse("2020-01-01T00:00:00Z"), updatedAt, null);
    }

    @Test
    void mapsItemToEntity() {
        var entity = mapper.toEntity(item("An example", "Java", new GitHubOwner("user123"), UPDATED));

        assertThat(entity.getGithubId()).isEqualTo(123456L);
        assertThat(entity.getOwnerLogin()).isEqualTo("user123");
        assertThat(entity.getStars()).isEqualTo(450);
        assertThat(entity.getForks()).isEqualTo(120);
        assertThat(entity.getLastUpdated()).isEqualTo(UPDATED);
    }

    @Test
    void keepsNullDescriptionAndLanguage() {
        var entity = mapper.toEntity(item(null, null, new GitHubOwner("user123"), UPDATED));

        assertThat(entity.getDescription()).isNull();
        assertThat(entity.getLanguage()).isNull();
    }

    @Test
    void responseUsesGitHubIdNotInternalId() {
        var response = mapper.toResponse(mapper.toEntity(item("An example", "Java", new GitHubOwner("user123"), UPDATED)));

        assertThat(response.id()).isEqualTo(123456L);
        assertThat(response.owner()).isEqualTo("user123");
        assertThat(response.lastUpdated()).isEqualTo(UPDATED);
    }

    @Test
    void itemsMissingRequiredFieldsAreIncomplete() {
        assertThat(GitHubRepoMapper.isComplete(item("d", "Java", new GitHubOwner("u"), UPDATED))).isTrue();
        assertThat(GitHubRepoMapper.isComplete(item("d", "Java", null, UPDATED))).isFalse();
        assertThat(GitHubRepoMapper.isComplete(item("d", "Java", new GitHubOwner(null), UPDATED))).isFalse();
        assertThat(GitHubRepoMapper.isComplete(item("d", "Java", new GitHubOwner("u"), null))).isFalse();
    }
}