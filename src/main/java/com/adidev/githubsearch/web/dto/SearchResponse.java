package com.adidev.githubsearch.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Result of a GitHub search; every returned repository has been upserted")
public record SearchResponse(
        @Schema(allowableValues = {"Repositories fetched and saved successfully", "No repositories found"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        String message,

        @Schema(description = "Repositories returned by GitHub; empty when nothing matched",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<RepositoryResponse> repositories) {
}