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
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/search")
    public SearchResponse search(@Valid @RequestBody SearchRequest request) {
        List<RepositoryResponse> repositories = searchService.searchAndStore(request.toCriteria())
                .stream().map(mapper::toResponse).toList();
        return new SearchResponse(repositories.isEmpty() ? EMPTY_MESSAGE : SAVED_MESSAGE, repositories);
    }

    @GetMapping("/repositories")
    public ResponseEntity<RepositoryListResponse> list(
            @RequestParam(name = "language", required = false) @Size(max = 50) String language,
            @RequestParam(name = "minStars", required = false) @Min(0) Integer minStars,
            @RequestParam(name = "sort", defaultValue = "stars")
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