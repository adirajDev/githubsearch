package com.adidev.githubsearch.github.exception;

public class GitHubTimeoutException extends GitHubApiException {
    public GitHubTimeoutException(Throwable cause) { super("GitHub did not respond in time", cause); }
}
