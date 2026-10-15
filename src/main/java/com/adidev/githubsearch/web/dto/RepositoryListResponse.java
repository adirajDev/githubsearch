package com.adidev.githubsearch.web.dto;

import java.util.List;

public record RepositoryListResponse(List<RepositoryResponse> repositories) {}