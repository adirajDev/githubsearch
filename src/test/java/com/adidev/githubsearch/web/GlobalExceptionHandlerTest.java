package com.adidev.githubsearch.web;

import com.adidev.githubsearch.github.exception.*;
import com.adidev.githubsearch.mapper.GitHubRepoMapper;
import com.adidev.githubsearch.service.RepositorySearchService;
import com.adidev.githubsearch.service.StoredRepositoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(GitHubRepositoryController.class)
@Import(GitHubRepoMapper.class)
class GlobalExceptionHandlerTest {

    @Autowired MockMvcTester mvc;
    @MockitoBean RepositorySearchService searchService;
    @MockitoBean StoredRepositoryService storedService;

    private MvcTestResult search(String body) {
        return mvc.post().uri("/api/github/search")
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    private MvcTestResult validSearch() {
        return search("{\"query\":\"spring\"}");
    }

    // ---------- client errors ----------

    @Test
    void blankQueryReturnsFieldError() {
        var result = search("{\"query\":\" \"}");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.errors.query").isEqualTo("must not be blank");
    }

    @Test
    void invalidSortInBodyReturnsFieldError() {
        var result = search("{\"query\":\"spring\",\"sort\":\"popularity\"}");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors.sort")
                .isEqualTo("must be one of: stars, forks, updated");
    }

    @Test
    void malformedJsonReturnsReadableMessage() {
        var result = search("{not json");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.detail")
                .isEqualTo("Request body is missing or is not valid JSON");
    }

    @Test
    void negativeMinStarsReturnsParameterError() {
        var result = mvc.get().uri("/api/github/repositories?minStars=-1").exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors.minStars")
                .isEqualTo("must be greater than or equal to 0");
    }

    @Test
    void nonNumericMinStarsReturnsParameterError() {
        var result = mvc.get().uri("/api/github/repositories?minStars=abc").exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors.minStars").isNotNull();
    }

    @Test
    void invalidSortParamReturnsParameterError() {
        var result = mvc.get().uri("/api/github/repositories?sort=popularity").exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors.sort")
                .isEqualTo("must be one of: stars, forks, updated");
    }

    // ---------- GitHub failures ----------

    @Test
    void rateLimitReturns429WithRetryAfter() {
        when(searchService.searchAndStore(any())).thenThrow(new GitHubRateLimitException(42));

        var result = validSearch();

        assertThat(result).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(result).headers().hasValue("Retry-After", "42");
        assertThat(result).bodyJson().extractingPath("$.retryAfterSeconds").isEqualTo(42);
    }

    @Test
    void gitHubRejectedQueryPassesReasonThrough() {
        when(searchService.searchAndStore(any()))
                .thenThrow(new GitHubInvalidQueryException("The search contains only logical operators"));

        var result = validSearch();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.detail")
                .isEqualTo("The search contains only logical operators");
    }

    @Test
    void timeoutReturns504() {
        when(searchService.searchAndStore(any())).thenThrow(new GitHubTimeoutException(null));

        assertThat(validSearch()).hasStatus(HttpStatus.GATEWAY_TIMEOUT);
    }

    @Test
    void upstreamFailureReturns502WithoutLeakingInternals() {
        when(searchService.searchAndStore(any()))
                .thenThrow(new GitHubUpstreamException("GitHub rejected the configured token (check GITHUB_TOKEN)"));

        var result = validSearch();

        assertThat(result).hasStatus(HttpStatus.BAD_GATEWAY);
        assertThat(result).bodyJson().extractingPath("$.detail").asString().doesNotContain("GITHUB_TOKEN");
    }

    // ---------- our own failures ----------

    @Test
    void databaseFailureReturns503() {
        when(storedService.find(any())).thenThrow(new DataAccessResourceFailureException("connection refused"));

        assertThat(mvc.get().uri("/api/github/repositories")).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void unexpectedErrorReturnsGeneric500() {
        when(searchService.searchAndStore(any())).thenThrow(new IllegalStateException("boom: internal detail"));

        var result = validSearch();

        assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result).bodyJson().extractingPath("$.detail").asString().doesNotContain("boom");
    }
}