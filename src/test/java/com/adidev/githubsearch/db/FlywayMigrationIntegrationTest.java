package com.adidev.githubsearch.db;

import com.adidev.githubsearch.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class FlywayMigrationIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE repositories RESTART IDENTITY");
    }

    private static final String INSERT = """
        INSERT INTO repositories (github_id, name, full_name, owner_login, html_url, last_updated)
        VALUES (?, 'demo', 'user/demo', 'user', 'https://github.com/user/demo', now())
        """;

    @Test
    void githubIdIsUnique() {
        jdbc.update(INSERT, 42L);
        assertThatThrownBy(() -> jdbc.update(INSERT, 42L))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void countersDefaultToZeroAndFetchedAtIsSet() {
        jdbc.update(INSERT, 1L);
        var row = jdbc.queryForMap("SELECT stars, forks, fetched_at FROM repositories WHERE github_id = 1");
        assertThat(row.get("stars")).isEqualTo(0);
        assertThat(row.get("forks")).isEqualTo(0);
        assertThat(row.get("fetched_at")).isNotNull();
    }

    @Test
    void filterIndexesExist() {
        List<String> indexes = jdbc.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'repositories'", String.class);
        assertThat(indexes).contains("idx_repositories_stars", "idx_repositories_language_lower");
    }
}