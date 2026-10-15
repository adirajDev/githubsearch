package com.adidev.githubsearch.web;

import com.adidev.githubsearch.domain.GitHubRepo;
import com.adidev.githubsearch.domain.RepositoryFilter;
import com.adidev.githubsearch.domain.SearchCriteria;
import com.adidev.githubsearch.domain.SortField;
import com.adidev.githubsearch.mapper.GitHubRepoMapper;
import com.adidev.githubsearch.service.RepositorySearchService;
import com.adidev.githubsearch.service.StoredRepositoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@WebMvcTest(GitHubRepositoryController.class)
@Import(GitHubRepoMapper.class)
class GitHubRepositoryControllerTest {

    @Autowired MockMvcTester mvc;
    @MockitoBean RepositorySearchService searchService;
    @MockitoBean StoredRepositoryService storedService;

    private static final GitHubRepo REPO = new GitHubRepo(123456L, "spring-boot-example",
            "user123/spring-boot-example", "An example repository for Spring Boot", "user123", "Java",
            "https://github.com/user123/spring-boot-example", 450, 120, 450, 3,
            null, Instant.parse("2024-01-01T12:00:00Z"), null);

    private static final String REPO_JSON = """
        {"id":123456,"name":"spring-boot-example","description":"An example repository for Spring Boot",
         "owner":"user123","language":"Java","stars":450,"forks":120,"lastUpdated":"2024-01-01T12:00:00Z"}
        """;

    // ---------- POST /api/github/search ----------

    @Test
    void searchReturnsSpecShape() {
        when(searchService.searchAndStore(any())).thenReturn(List.of(REPO));

        assertThat(mvc.post().uri("/api/github/search").contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"spring boot\",\"language\":\"Java\",\"sort\":\"stars\"}"))
                .hasStatusOk()
                .bodyJson().isStrictlyEqualTo("""
                {"message":"Repositories fetched and saved successfully","repositories":[%s]}
                """.formatted(REPO_JSON));
    }

    @Test
    void searchPassesCriteriaToService() {
        when(searchService.searchAndStore(any())).thenReturn(List.of());

        assertThat(mvc.post().uri("/api/github/search").contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"spring\",\"sort\":\"FORKS\"}"))
                .hasStatusOk();

        ArgumentCaptor<SearchCriteria> captor = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(searchService).searchAndStore(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new SearchCriteria("spring", null, SortField.FORKS));
    }

    @Test
    void emptySearchResultIsSuccessWithMessage() {
        when(searchService.searchAndStore(any())).thenReturn(List.of());

        assertThat(mvc.post().uri("/api/github/search").contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"zzzz-no-match\"}"))
                .hasStatusOk()
                .bodyJson().isStrictlyEqualTo("{\"message\":\"No repositories found\",\"repositories\":[]}");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"query\":\"\"}",
            "{\"language\":\"Java\"}",
            "{\"query\":\"spring\",\"sort\":\"popularity\"}",
            "{not json",
            ""
    })
    void invalidSearchBodiesAreRejectedBeforeReachingService(String body) {
        assertThat(mvc.post().uri("/api/github/search").contentType(MediaType.APPLICATION_JSON).content(body))
                .hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(searchService);
    }

    // ---------- GET /api/github/repositories ----------

    @Test
    void listUsesDefaultsAndReturnsSpecShape() {
        when(storedService.find(any())).thenReturn(new PageImpl<>(List.of(REPO), PageRequest.of(0, 30), 1));

        var result = mvc.get().uri("/api/github/repositories").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).headers().hasValue("X-Total-Count", "1");
        assertThat(result).bodyJson().isStrictlyEqualTo("{\"repositories\":[%s]}".formatted(REPO_JSON));

        ArgumentCaptor<RepositoryFilter> captor = ArgumentCaptor.forClass(RepositoryFilter.class);
        verify(storedService).find(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new RepositoryFilter(null, null, SortField.STARS, 0, 30));
    }

    @Test
    void listPassesAllFiltersToService() {
        when(storedService.find(any())).thenReturn(new PageImpl<>(List.of()));

        assertThat(mvc.get().uri("/api/github/repositories?language=Java&minStars=100&sort=forks&page=1&size=10"))
                .hasStatusOk();

        ArgumentCaptor<RepositoryFilter> captor = ArgumentCaptor.forClass(RepositoryFilter.class);
        verify(storedService).find(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new RepositoryFilter("Java", 100, SortField.FORKS, 1, 10));
    }

    @ParameterizedTest
    @ValueSource(strings = {"minStars=-1", "minStars=abc", "size=0", "size=101", "page=-1", "sort=popularity"})
    void invalidQueryParamsAreRejectedBeforeReachingService(String params) {
        assertThat(mvc.get().uri("/api/github/repositories?" + params))
                .hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(storedService);
    }
}