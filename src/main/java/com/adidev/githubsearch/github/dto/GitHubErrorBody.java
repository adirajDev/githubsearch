package com.adidev.githubsearch.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubErrorBody(String message, List<Detail> errors) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Detail(String message, String code, String field) {}
}