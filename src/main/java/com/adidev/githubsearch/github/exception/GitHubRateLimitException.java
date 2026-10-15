package com.adidev.githubsearch.github.exception;

public class GitHubRateLimitException extends GitHubApiException {
    private final long retryAfterSeconds;

    public GitHubRateLimitException(long retryAfterSeconds) {
        super("GitHub API rate limit exceeded", null);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() { return retryAfterSeconds; }
}
