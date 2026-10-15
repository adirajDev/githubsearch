package com.adidev.githubsearch.repository;

import com.adidev.githubsearch.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class GitHubRepoMappingIntegrationTest {

    @Autowired GitHubRepoRepository repository;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE repositories RESTART IDENTITY");
    }

    @Test
    void mapsAllColumnsIncludingNullableOnes() {
        jdbc.update("""
            INSERT INTO repositories (github_id, name, full_name, owner_login, html_url, stars, forks, last_updated)
            VALUES (7, 'demo', 'user/demo', 'user', 'https://github.com/user/demo', 450, 120, '2024-01-01T12:00:00Z')
            """);

        var repo = repository.findByGithubId(7L).orElseThrow();

        assertThat(repo.getName()).isEqualTo("demo");
        assertThat(repo.getOwnerLogin()).isEqualTo("user");
        assertThat(repo.getStars()).isEqualTo(450);
        assertThat(repo.getDescription()).isNull();
        assertThat(repo.getLanguage()).isNull();
        assertThat(repo.getLastUpdated()).hasToString("2024-01-01T12:00:00Z");
    }
}
