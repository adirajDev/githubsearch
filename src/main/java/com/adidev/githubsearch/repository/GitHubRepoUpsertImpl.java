package com.adidev.githubsearch.repository;

import com.adidev.githubsearch.domain.GitHubRepo;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class GitHubRepoUpsertImpl implements GitHubRepoUpsert {

    private static final String UPSERT_SQL = """
        INSERT INTO repositories (github_id, name, full_name, description, owner_login, language, html_url,
                                  stars, forks, watchers, open_issues, created_at, last_updated, pushed_at, fetched_at)
        VALUES (:githubId, :name, :fullName, :description, :ownerLogin, :language, :htmlUrl,
                :stars, :forks, :watchers, :openIssues, :createdAt, :lastUpdated, :pushedAt, now())
        ON CONFLICT (github_id) DO UPDATE SET
            name         = EXCLUDED.name,
            full_name    = EXCLUDED.full_name,
            description  = EXCLUDED.description,
            owner_login  = EXCLUDED.owner_login,
            language     = EXCLUDED.language,
            html_url     = EXCLUDED.html_url,
            stars        = EXCLUDED.stars,
            forks        = EXCLUDED.forks,
            watchers     = EXCLUDED.watchers,
            open_issues  = EXCLUDED.open_issues,
            created_at   = EXCLUDED.created_at,
            last_updated = EXCLUDED.last_updated,
            pushed_at    = EXCLUDED.pushed_at,
            fetched_at   = EXCLUDED.fetched_at
        """;

    private final NamedParameterJdbcTemplate jdbc;

    public GitHubRepoUpsertImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public int upsertAll(Collection<GitHubRepo> repos) {
        if (repos == null || repos.isEmpty()) {
            return 0;
        }
        // Defensive dedupe: last occurrence wins, original order kept.
        Map<Long, GitHubRepo> unique = new LinkedHashMap<>();
        repos.forEach(r -> unique.put(r.getGithubId(), r));

        SqlParameterSource[] batch = unique.values().stream()
                .map(GitHubRepoUpsertImpl::toParams)
                .toArray(SqlParameterSource[]::new);

        jdbc.batchUpdate(UPSERT_SQL, batch);
        return batch.length;
    }

    private static SqlParameterSource toParams(GitHubRepo r) {
        return new MapSqlParameterSource()
                .addValue("githubId", r.getGithubId())
                .addValue("name", r.getName())
                .addValue("fullName", r.getFullName())
                .addValue("description", r.getDescription(), Types.VARCHAR)
                .addValue("ownerLogin", r.getOwnerLogin())
                .addValue("language", r.getLanguage(), Types.VARCHAR)
                .addValue("htmlUrl", r.getHtmlUrl())
                .addValue("stars", r.getStars())
                .addValue("forks", r.getForks())
                .addValue("watchers", r.getWatchers())
                .addValue("openIssues", r.getOpenIssues())
                .addValue("createdAt", utc(r.getCreatedAt()), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("lastUpdated", utc(r.getLastUpdated()), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("pushedAt", utc(r.getPushedAt()), Types.TIMESTAMP_WITH_TIMEZONE);
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}