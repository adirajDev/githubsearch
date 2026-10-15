package com.adidev.githubsearch.github;

import com.adidev.githubsearch.github.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RestGitHubSearchClientTest {

    private static final String BASE = "https://api.github.com";
    private static final Instant NOW = Instant.parse("2026-10-15T10:00:00Z");
    private static final String EMPTY_RESULT =
            "{\"total_count\":0,\"incomplete_results\":false,\"items\":[]}";

    private static final String ONE_RESULT = """
        {"total_count":1,"incomplete_results":false,"items":[{
          "id":123456,"name":"spring-boot-example","full_name":"user123/spring-boot-example",
          "description":null,"owner":{"login":"user123"},"language":"Java",
          "html_url":"https://github.com/user123/spring-boot-example",
          "stargazers_count":450,"forks_count":120,"watchers_count":450,"open_issues_count":3,
          "created_at":"2020-01-01T00:00:00Z","updated_at":"2024-01-01T12:00:00Z",
          "pushed_at":"2024-01-01T11:00:00Z","some_field_we_dont_map":"ignored"}]}
        """;

    private final GitHubSearchQuery query =
            new GitHubSearchQuery("spring boot in:name language:Java", "stars", "desc", 30);

    private MockRestServiceServer server;
    private RestGitHubSearchClient client;

    @BeforeEach
    void setUp() {
        var props = new GitHubProperties(BASE, "test-token", Duration.ofSeconds(1), Duration.ofSeconds(1));
        RestClient.Builder builder = GitHubClientConfig.restClientBuilder(props);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestGitHubSearchClient(builder.build(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private ResponseActions expectSearch() {
        return server.expect(requestTo(startsWith(BASE + "/search/repositories")));
    }

    @Test
    void parsesResultsAndSendsExpectedRequest() {
        expectSearch()
                .andExpect(method(GET))
                .andExpect(queryParam("sort", "stars"))
                .andExpect(queryParam("order", "desc"))
                .andExpect(queryParam("per_page", "30"))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andRespond(withSuccess(ONE_RESULT, MediaType.APPLICATION_JSON));

        var response = client.search(query);

        var item = response.items().getFirst();
        assertThat(item.id()).isEqualTo(123456L);
        assertThat(item.owner().login()).isEqualTo("user123");
        assertThat(item.stargazersCount()).isEqualTo(450);
        assertThat(item.description()).isNull();
        assertThat(item.updatedAt()).isEqualTo(Instant.parse("2024-01-01T12:00:00Z"));
        server.verify();
    }

    @Test
    void zeroResultsIsNotAnError() {
        expectSearch().andRespond(withSuccess(EMPTY_RESULT, MediaType.APPLICATION_JSON));

        assertThat(client.search(query).items()).isEmpty();
    }

    @Test
    void encodesPlusSignsSoCppIsNotReadAsSpaces() {
        expectSearch()
                .andExpect(requestTo(containsString("C%2B%2B")))
                .andRespond(withSuccess(EMPTY_RESULT, MediaType.APPLICATION_JSON));

        client.search(new GitHubSearchQuery("engine in:name language:C++", "stars", "desc", 30));
        server.verify();
    }

    @Test
    void primaryRateLimitUsesResetHeader() {
        expectSearch().andRespond(withStatus(HttpStatus.FORBIDDEN)
                .header("X-RateLimit-Remaining", "0")
                .header("X-RateLimit-Reset", String.valueOf(NOW.getEpochSecond() + 120)));

        assertThatThrownBy(() -> client.search(query))
                .isInstanceOfSatisfying(GitHubRateLimitException.class,
                        e -> assertThat(e.getRetryAfterSeconds()).isEqualTo(120));
    }

    @Test
    void secondaryRateLimitUsesRetryAfterHeader() {
        expectSearch().andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "30"));

        assertThatThrownBy(() -> client.search(query))
                .isInstanceOfSatisfying(GitHubRateLimitException.class,
                        e -> assertThat(e.getRetryAfterSeconds()).isEqualTo(30));
    }

    @Test
    void forbiddenWithoutRateLimitHeadersIsUpstreamError() {
        expectSearch().andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> client.search(query)).isInstanceOf(GitHubUpstreamException.class);
    }

    @Test
    void invalidQueryPassesGitHubReasonThrough() {
        expectSearch().andRespond(withStatus(HttpStatusCode.valueOf(422))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                {"message":"Validation Failed","errors":[{"message":"The search contains only logical operators"}]}
                """));

        assertThatThrownBy(() -> client.search(query))
                .isInstanceOf(GitHubInvalidQueryException.class)
                .hasMessage("The search contains only logical operators");
    }

    @Test
    void serverErrorIsUpstreamError() {
        expectSearch().andRespond(withServerError());

        assertThatThrownBy(() -> client.search(query)).isInstanceOf(GitHubUpstreamException.class);
    }

    @Test
    void timeoutIsTranslated() {
        expectSearch().andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> client.search(query)).isInstanceOf(GitHubTimeoutException.class);
    }

    @Test
    void malformedJsonIsUpstreamError() {
        expectSearch().andRespond(withSuccess("this is not json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.search(query)).isInstanceOf(GitHubUpstreamException.class);
    }

    @Test
    void responseWithoutItemsIsUpstreamError() {
        expectSearch().andRespond(withSuccess(
                "{\"total_count\":0,\"incomplete_results\":false}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.search(query))
                .isInstanceOf(GitHubUpstreamException.class)
                .hasMessageContaining("empty or malformed");
    }

    @Test
    void responseMissingRequiredFieldsIsUpstreamError() {
        expectSearch().andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.search(query))
                .isInstanceOf(GitHubUpstreamException.class)
                .hasMessageContaining("unreadable");
    }
}