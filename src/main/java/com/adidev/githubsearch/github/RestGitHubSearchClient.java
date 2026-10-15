package com.adidev.githubsearch.github;

import com.adidev.githubsearch.github.dto.GitHubErrorBody;
import com.adidev.githubsearch.github.dto.GitHubSearchResponse;
import com.adidev.githubsearch.github.exception.*;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Clock;

@Component
public class RestGitHubSearchClient implements GitHubSearchClient {

    static final long DEFAULT_RETRY_AFTER_SECONDS = 60;

    private final RestClient restClient;
    private final Clock clock;

    public RestGitHubSearchClient(RestClient gitHubRestClient, Clock clock) {
        this.restClient = gitHubRestClient;
        this.clock = clock;
    }

    @Override
    public GitHubSearchResponse search(GitHubSearchQuery query) {
        GitHubSearchResponse body;
        try {
            body = restClient.get()
                    .uri(b -> b.path("/search/repositories")
                            .queryParam("q", "{q}")
                            .queryParam("sort", "{sort}")
                            .queryParam("order", "{order}")
                            .queryParam("per_page", "{perPage}")
                            .build(query.q(), query.sort(), query.order(), query.perPage()))
                    .retrieve()
                    .body(GitHubSearchResponse.class);
        } catch (RestClientResponseException ex) {
            throw translate(ex);
        } catch (ResourceAccessException ex) {
            if (isTimeout(ex.getCause())) {
                throw new GitHubTimeoutException(ex);
            }
            throw new GitHubUpstreamException("Could not reach GitHub", ex);
        } catch (RestClientException ex) {
            throw new GitHubUpstreamException("GitHub returned an unreadable response", ex);
        }

        if (body == null || body.items() == null) {
            throw new GitHubUpstreamException("GitHub returned an empty or malformed response");
        }
        return body;
    }

    private GitHubApiException translate(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        HttpHeaders headers = ex.getResponseHeaders();

        if (status == 429 || (status == 403 && isRateLimited(headers))) {
            return new GitHubRateLimitException(retryAfterSeconds(headers));
        }
        if (status == 422) {
            return new GitHubInvalidQueryException(errorMessage(ex));
        }
        if (status == 401) {
            return new GitHubUpstreamException("GitHub rejected the configured token (check GITHUB_TOKEN)", ex);
        }
        return new GitHubUpstreamException("GitHub responded with status " + status, ex);
    }

    private static boolean isRateLimited(HttpHeaders headers) {
        return headers != null
                && ("0".equals(headers.getFirst("X-RateLimit-Remaining"))
                || headers.getFirst(HttpHeaders.RETRY_AFTER) != null);
    }

    private long retryAfterSeconds(HttpHeaders headers) {
        if (headers == null) {
            return DEFAULT_RETRY_AFTER_SECONDS;
        }
        try {
            String retryAfter = headers.getFirst(HttpHeaders.RETRY_AFTER);
            if (retryAfter != null) {
                return Math.max(0, Long.parseLong(retryAfter.trim()));
            }
            String reset = headers.getFirst("X-RateLimit-Reset");
            if (reset != null) {
                return Math.max(0, Long.parseLong(reset.trim()) - clock.instant().getEpochSecond());
            }
        } catch (NumberFormatException ignored) {
            // fall through to the default
        }
        return DEFAULT_RETRY_AFTER_SECONDS;
    }

    private static String errorMessage(RestClientResponseException ex) {
        GitHubErrorBody body;
        try {
            body = ex.getResponseBodyAs(GitHubErrorBody.class);
        } catch (RuntimeException unreadable) {
            body = null;
        }
        if (body != null && body.errors() != null && !body.errors().isEmpty()
                && body.errors().getFirst().message() != null) {
            return body.errors().getFirst().message();
        }
        if (body != null && body.message() != null) {
            return body.message();
        }
        return "GitHub rejected the search query";
    }

    private static boolean isTimeout(Throwable cause) {
        return cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException;
    }
}