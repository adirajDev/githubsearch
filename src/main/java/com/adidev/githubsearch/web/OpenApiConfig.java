package com.adidev.githubsearch.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI gitHubSearchOpenApi() {
        return new OpenAPI().info(new Info()
                .title("GitHub Repository Searcher API")
                .version("v1")
                .description("""
                        Searches GitHub repositories, stores the results in PostgreSQL \
                        (upserted by GitHub repository id, so repeat searches never duplicate rows), \
                        and queries the stored results with optional filters.

                        All errors are returned as RFC 9457 Problem Details \
                        (`application/problem+json`)."""));
    }
}