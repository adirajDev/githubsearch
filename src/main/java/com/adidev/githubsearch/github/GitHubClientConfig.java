package com.adidev.githubsearch.github;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class GitHubClientConfig {

    @Bean
    RestClient gitHubRestClient(GitHubProperties props) {
        return restClientBuilder(props).build();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /** Shared by the bean above and the tests, so tests exercise the real headers and timeouts. */
    static RestClient.Builder restClientBuilder(GitHubProperties props) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(props.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(props.readTimeout());

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader(HttpHeaders.USER_AGENT, "github-repo-searcher");

        if (StringUtils.hasText(props.token())) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.token());
        }
        return builder;
    }
}