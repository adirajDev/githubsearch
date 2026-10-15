package com.adidev.githubsearch.github.exception;

public class GitHubUpstreamException extends GitHubApiException {
    public GitHubUpstreamException(String message) { super(message, null); }
    public GitHubUpstreamException(String message, Throwable cause) { super(message, cause); }
}
