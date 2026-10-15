package com.adidev.githubsearch.github.exception;

public abstract class GitHubApiException extends RuntimeException {
    protected GitHubApiException(String message, Throwable cause) { super(message, cause); }
}