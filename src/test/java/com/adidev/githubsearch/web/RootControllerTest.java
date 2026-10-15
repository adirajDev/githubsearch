package com.adidev.githubsearch.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(RootController.class)
class RootControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void rootRedirectsToSwaggerUi() {
        assertThat(mvc.get().uri("/"))
                .hasStatus3xxRedirection()
                .hasRedirectedUrl("/swagger-ui/index.html");
    }
}