package com.adidev.githubsearch.web.dto;

import java.util.List;

public record SearchResponse(String message, List<RepositoryResponse> repositories) {}