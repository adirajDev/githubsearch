package com.adidev.githubsearch.web;

/** Example bodies for OpenAPI docs. Captured from real responses; keep in sync with GlobalExceptionHandler. */
final class ApiExamples {

    private ApiExamples() {}

    static final String PROBLEM_JSON = "application/problem+json";

    static final String BODY_VALIDATION = """
            {
              "type": "about:blank",
              "title": "Bad Request",
              "status": 400,
              "detail": "Request validation failed",
              "instance": "/api/github/search",
              "errors": {
                "query": "must not be blank",
                "sort": "sort must be one of: stars, forks, updated"
              }
            }""";

    static final String MALFORMED_JSON = """
            {
              "type": "about:blank",
              "title": "Bad Request",
              "status": 400,
              "detail": "Request body is missing or is not valid JSON",
              "instance": "/api/github/search"
            }""";

    static final String GITHUB_REJECTED_QUERY = """
            {
              "type": "about:blank",
              "title": "Invalid search query",
              "status": 400,
              "detail": "Validation Failed",
              "instance": "/api/github/search"
            }""";

    static final String PARAM_VALIDATION = """
            {
              "type": "about:blank",
              "title": "Bad Request",
              "status": 400,
              "detail": "Request validation failed",
              "instance": "/api/github/repositories",
              "errors": {
                "minStars": "must be greater than or equal to 0",
                "size": "must be less than or equal to 100"
              }
            }""";

    static final String RATE_LIMITED = """
            {
              "type": "about:blank",
              "title": "Too Many Requests",
              "status": 429,
              "detail": "GitHub API rate limit exceeded. Retry after the indicated delay.",
              "instance": "/api/github/search",
              "retryAfterSeconds": 42
            }""";

    static final String BAD_GATEWAY = """
            {
              "type": "about:blank",
              "title": "Bad Gateway",
              "status": 502,
              "detail": "GitHub API returned an unexpected response",
              "instance": "/api/github/search"
            }""";

    static final String GATEWAY_TIMEOUT = """
            {
              "type": "about:blank",
              "title": "Gateway Timeout",
              "status": 504,
              "detail": "GitHub API did not respond in time",
              "instance": "/api/github/search"
            }""";

    static final String DB_UNAVAILABLE = """
            {
              "type": "about:blank",
              "title": "Service Unavailable",
              "status": 503,
              "detail": "Database is temporarily unavailable",
              "instance": "/api/github/repositories"
            }""";

    static final String INTERNAL_ERROR = """
            {
              "type": "about:blank",
              "title": "Internal Server Error",
              "status": 500,
              "detail": "An unexpected error occurred",
              "instance": "/api/github/search"
            }""";
}