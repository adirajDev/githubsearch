package com.adidev.githubsearch.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

@Entity
@Immutable
@Table(name = "repositories")
public class GitHubRepo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "github_id", nullable = false, unique = true)
    private Long githubId;

    @Column(nullable = false)
    private String name;

    @Column(name = "full_name", nullable = false, length = 512)
    private String fullName;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "owner_login", nullable = false)
    private String ownerLogin;

    @Column(length = 100)
    private String language;

    @Column(name = "html_url", nullable = false, columnDefinition = "text")
    private String htmlUrl;

    @Column(nullable = false) private int stars;
    @Column(nullable = false) private int forks;
    @Column(nullable = false) private int watchers;
    @Column(name = "open_issues", nullable = false) private int openIssues;

    @Column(name = "created_at")   private Instant createdAt;
    @Column(name = "last_updated", nullable = false) private Instant lastUpdated;
    @Column(name = "pushed_at")    private Instant pushedAt;
    @Column(name = "fetched_at", nullable = false, insertable = false, updatable = false)
    private Instant fetchedAt;

    protected GitHubRepo() { } // required by JPA

    public GitHubRepo(Long githubId, String name, String fullName, String description,
                      String ownerLogin, String language, String htmlUrl,
                      int stars, int forks, int watchers, int openIssues,
                      Instant createdAt, Instant lastUpdated, Instant pushedAt) {
        this.githubId = githubId;
        this.name = name;
        this.fullName = fullName;
        this.description = description;
        this.ownerLogin = ownerLogin;
        this.language = language;
        this.htmlUrl = htmlUrl;
        this.stars = stars;
        this.forks = forks;
        this.watchers = watchers;
        this.openIssues = openIssues;
        this.createdAt = createdAt;
        this.lastUpdated = lastUpdated;
        this.pushedAt = pushedAt;
    }

    public Long getId() { return id; }
    public Long getGithubId() { return githubId; }
    public String getName() { return name; }
    public String getFullName() { return fullName; }
    public String getDescription() { return description; }
    public String getOwnerLogin() { return ownerLogin; }
    public String getLanguage() { return language; }
    public String getHtmlUrl() { return htmlUrl; }
    public int getStars() { return stars; }
    public int getForks() { return forks; }
    public int getWatchers() { return watchers; }
    public int getOpenIssues() { return openIssues; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUpdated() { return lastUpdated; }
    public Instant getPushedAt() { return pushedAt; }
    public Instant getFetchedAt() { return fetchedAt; }
}