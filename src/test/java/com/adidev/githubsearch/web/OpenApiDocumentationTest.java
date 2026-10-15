package com.adidev.githubsearch.web;

import com.adidev.githubsearch.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import com.adidev.githubsearch.domain.SortField;
import java.util.Arrays;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiDocumentationTest {

    @Autowired
    MockMvcTester mvc;

    private MvcTestResult apiDocs() {
        return mvc.get().uri("/v3/api-docs").exchange();
    }

    @Test
    void searchRequestSortIsDocumentedAsTheSortFieldValues() {
        var expected = Arrays.stream(SortField.values())
                .map(f -> f.name().toLowerCase(Locale.ROOT))
                .toList();

        assertThat(apiDocs()).bodyJson()
                .extractingPath("$.components.schemas.SearchRequest.properties.sort.enum")
                .asArray().containsExactlyElementsOf(expected);
    }

    @Test
    void searchRequestHasUsableExamples() {
        assertThat(apiDocs()).bodyJson()
                .extractingPath("$.components.schemas.SearchRequest.properties.query.example")
                .isEqualTo("spring boot");
    }

    @Test
    void repositoryResponseDocumentsGitHubIdAndAlwaysPresentFields() {
        var result = apiDocs();

        assertThat(result).bodyJson()
                .extractingPath("$.components.schemas.RepositoryResponse.properties.id.description")
                .asString().containsIgnoringCase("GitHub");
        assertThat(result).bodyJson()
                .extractingPath("$.components.schemas.RepositoryResponse.required")
                .asArray().containsExactlyInAnyOrder(
                        "id", "name", "description", "owner",
                        "language", "stars", "forks", "lastUpdated");
        assertThat(result).bodyJson()
                .extractingPath("$.components.schemas.RepositoryResponse.properties.lastUpdated.format")
                .isEqualTo("date-time");
    }

    @Test
    void publishesOpenApiSpecWithMetadataAndBothEndpoints() {
        MvcTestResult result = mvc.get().uri("/v3/api-docs").exchange();

        assertThat(result).hasStatusOk()
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON);
        assertThat(result).bodyJson()
                .extractingPath("$.info.title").isEqualTo("GitHub Repository Searcher API");
        assertThat(result).bodyJson()
                .extractingPath("$.paths['/api/github/search'].post").isNotNull();
        assertThat(result).bodyJson()
                .extractingPath("$.paths['/api/github/repositories'].get").isNotNull();
    }

    @Test
    void servesSwaggerUi() {
        assertThat(mvc.get().uri("/swagger-ui/index.html")).hasStatusOk();
    }

    @Test
    void searchDocumentsOnlyTheResponsesItCanReturn() {
        assertThat(apiDocs()).bodyJson()
                .extractingPath("$.paths['/api/github/search'].post.responses")
                .asMap().containsOnlyKeys("200", "400", "429", "500", "502", "503", "504");
    }

    @Test
    void storedRepositoriesDocumentsNoGitHubErrors() {
        assertThat(apiDocs()).bodyJson()
                .extractingPath("$.paths['/api/github/repositories'].get.responses")
                .asMap().containsOnlyKeys("200", "400", "500", "503");
    }

    @Test
    void errorsAreDocumentedAsProblemJsonWithHeadersWhereRelevant() {
        var result = apiDocs();

        assertThat(result).bodyJson()
                .extractingPath("$.paths['/api/github/search'].post.responses['400'].content['application/problem+json']")
                .isNotNull();
        assertThat(result).bodyJson()
                .extractingPath("$.paths['/api/github/search'].post.responses['429'].headers['Retry-After']")
                .isNotNull();
        assertThat(result).bodyJson()
                .extractingPath("$.paths['/api/github/repositories'].get.responses['200'].headers['X-Total-Count']")
                .isNotNull();
    }

    @Test
    void storedRepositoriesDocumentsAllQueryParameters() {
        var result = apiDocs();

        assertThat(result).bodyJson()
                .extractingPath("$.paths['/api/github/repositories'].get.parameters[*].name")
                .asArray().containsExactlyInAnyOrder("language", "minStars", "sort", "page", "size");
        assertThat(result).bodyJson()
                .extractingPath("$.paths['/api/github/repositories'].get.parameters[?(@.name=='sort')].schema.enum[*]")
                .asArray().containsExactlyInAnyOrder("stars", "forks", "updated");
    }

    @Test
    void operationsHaveStableIds() {
        var result = apiDocs();

        assertThat(result).bodyJson()
                .extractingPath("$.paths['/api/github/search'].post.operationId")
                .isEqualTo("searchRepositories");
        assertThat(result).bodyJson()
                .extractingPath("$.paths['/api/github/repositories'].get.operationId")
                .isEqualTo("listStoredRepositories");
    }

    @Test
    void serverUrlHonoursForwardedHeadersFromTheProxy() {
        var result = mvc.get().uri("/v3/api-docs")
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Host", "githubsearch.onrender.com")
                .exchange();

        assertThat(result).bodyJson()
                .extractingPath("$.servers[0].url")
                .isEqualTo("https://githubsearch.onrender.com");
    }
}