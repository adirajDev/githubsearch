package com.adidev.githubsearch.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "A GitHub repository as stored by this service")
public record RepositoryResponse(

        @Schema(description = "GitHub's repository id (unique; not this service's internal database id)",
                example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(example = "spring-boot-example", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "Repository description; null when the repository has none",
                example = "An example repository for Spring Boot",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String description,

        @Schema(description = "Login of the repository owner",
                example = "user123", requiredMode = Schema.RequiredMode.REQUIRED)
        String owner,

        @Schema(description = "Primary language as detected by GitHub; null when undetected",
                example = "Java", requiredMode = Schema.RequiredMode.REQUIRED)
        String language,

        @Schema(example = "450", requiredMode = Schema.RequiredMode.REQUIRED)
        int stars,

        @Schema(example = "120", requiredMode = Schema.RequiredMode.REQUIRED)
        int forks,

        @Schema(description = "Last update time from GitHub, ISO-8601 in UTC",
                example = "2024-01-01T12:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant lastUpdated) {
}