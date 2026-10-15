package com.adidev.githubsearch.web;

import com.adidev.githubsearch.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiDocumentationTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void publishesOpenApiSpecWithMetadataAndBothEndpoints() {
        MvcTestResult result = mvc.get().uri("/v3/api-docs").exchange();

        assertThat(result).hasStatusOk()
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON);
        assertThat(result).bodyJson()
                .extractingPath("$.info.title").isEqualTo("GitHub Repository Searcher API");
        assertThat(result).bodyJson()
                .extractingPath("$.paths['/api/github/search'].post").isNotNull();
        assertThat(result).bodyJson()
                .extractingPath("$.paths['/api/github/repositories'].get").isNotNull();
    }

    @Test
    void servesSwaggerUi() {
        assertThat(mvc.get().uri("/swagger-ui/index.html")).hasStatusOk();
    }
}