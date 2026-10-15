package com.adidev.githubsearch.web;

import com.adidev.githubsearch.domain.GitHubRepo;
import com.adidev.githubsearch.domain.RepositoryFilter;
import com.adidev.githubsearch.domain.SortField;
import com.adidev.githubsearch.mapper.GitHubRepoMapper;
import com.adidev.githubsearch.service.RepositorySearchService;
import com.adidev.githubsearch.service.StoredRepositoryService;
import com.adidev.githubsearch.web.dto.RepositoryListResponse;
import com.adidev.githubsearch.web.dto.RepositoryResponse;
import com.adidev.githubsearch.web.dto.SearchRequest;
import com.adidev.githubsearch.web.dto.SearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "GitHub repositories",
        description = "Search GitHub and query repositories stored from earlier searches")
@RestController
@RequestMapping("/api/github")
public class GitHubRepositoryController {

    static final String SAVED_MESSAGE = "Repositories fetched and saved successfully";
    static final String EMPTY_MESSAGE = "No repositories found";

    private final RepositorySearchService searchService;
    private final StoredRepositoryService storedService;
    private final GitHubRepoMapper mapper;

    public GitHubRepositoryController(RepositorySearchService searchService,
                                      StoredRepositoryService storedService,
                                      GitHubRepoMapper mapper) {
        this.searchService = searchService;
        this.storedService = storedService;
        this.mapper = mapper;
    }

    @Operation(
            operationId = "searchRepositories",
            summary = "Search GitHub and store the results",
            description = """
                    Searches GitHub for repositories whose **name** matches `query` \
                    (optionally filtered by `language`), returns the first page of up to 30 results, \
                    and upserts each one by its GitHub id, so repeating a search updates rows instead of duplicating them.

                    Returns **200** rather than 201: the call updates existing rows and creates no single new resource. \
                    An empty result is a 200 with the message "No repositories found", not an error.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search completed; results stored"),
            @ApiResponse(responseCode = "400",
                    description = "Invalid request body, malformed JSON, or a query GitHub rejected",
                    content = @Content(mediaType = ApiExamples.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = {
                                    @ExampleObject(name = "Validation failed", value = ApiExamples.BODY_VALIDATION),
                                    @ExampleObject(name = "Malformed JSON", value = ApiExamples.MALFORMED_JSON),
                                    @ExampleObject(name = "Rejected by GitHub", value = ApiExamples.GITHUB_REJECTED_QUERY)
                            })),
            @ApiResponse(responseCode = "429", description = "GitHub rate limit reached",
                    headers = @Header(name = "Retry-After", description = "Seconds until GitHub accepts requests again",
                            schema = @Schema(type = "integer")),
                    content = @Content(mediaType = ApiExamples.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = ApiExamples.RATE_LIMITED))),
            @ApiResponse(responseCode = "502", description = "GitHub returned an error or an unreadable response",
                    content = @Content(mediaType = ApiExamples.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = ApiExamples.BAD_GATEWAY))),
            @ApiResponse(responseCode = "504", description = "GitHub did not respond in time",
                    content = @Content(mediaType = ApiExamples.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = ApiExamples.GATEWAY_TIMEOUT))),
            @ApiResponse(responseCode = "503", description = "Database unavailable",
                    content = @Content(mediaType = ApiExamples.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = ApiExamples.DB_UNAVAILABLE))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(mediaType = ApiExamples.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = ApiExamples.INTERNAL_ERROR)))
    })
    @PostMapping("/search")
    public SearchResponse search(@Valid @RequestBody SearchRequest request) {
        List<RepositoryResponse> repositories = searchService.searchAndStore(request.toCriteria())
                .stream().map(mapper::toResponse).toList();
        return new SearchResponse(repositories.isEmpty() ? EMPTY_MESSAGE : SAVED_MESSAGE, repositories);
    }

    @Operation(
            operationId = "listStoredRepositories",
            summary = "List stored repositories",
            description = "Reads repositories stored by earlier searches and applies optional filters. "
                    + "Results are sorted descending by the selected field, with GitHub id as a stable tie-breaker.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matching stored repositories",
                    headers = @Header(name = "X-Total-Count",
                            description = "Total matching rows across all pages",
                            schema = @Schema(type = "integer", format = "int64"))),
            @ApiResponse(responseCode = "400", description = "Invalid query parameter",
                    content = @Content(mediaType = ApiExamples.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = ApiExamples.PARAM_VALIDATION))),
            @ApiResponse(responseCode = "503", description = "Database unavailable",
                    content = @Content(mediaType = ApiExamples.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = ApiExamples.DB_UNAVAILABLE))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(mediaType = ApiExamples.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = ApiExamples.INTERNAL_ERROR)))
    })
    @GetMapping("/repositories")
    public ResponseEntity<RepositoryListResponse> list(
            @RequestParam(name = "language", required = false) @Size(max = 50) String language,
            @RequestParam(name = "minStars", required = false) @Min(0) Integer minStars,
            @RequestParam(name = "sort", defaultValue = "stars")
            @Parameter(
                    description = "Sort order, always descending.",
                    schema = @Schema(type = "string", allowableValues = {"stars", "forks", "updated"},
                            defaultValue = "stars"))
            @Pattern(regexp = SortField.PATTERN, message = SortField.ALLOWED_MESSAGE) String sort,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "30") @Min(1) @Max(RepositoryFilter.MAX_SIZE) int size) {

        Page<GitHubRepo> result = storedService.find(
                new RepositoryFilter(language, minStars, SortField.fromValue(sort), page, size));

        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(new RepositoryListResponse(result.map(mapper::toResponse).getContent()));
    }
}
