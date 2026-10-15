package com.adidev.githubsearch.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Stored repositories matching the filters, in the requested order")
public record RepositoryListResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<RepositoryResponse> repositories) {
}