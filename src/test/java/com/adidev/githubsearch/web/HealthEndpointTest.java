package com.adidev.githubsearch.web;

import com.adidev.githubsearch.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class HealthEndpointTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void healthIsUpWhenDatabaseIsReachable() {
        assertThat(mvc.get().uri("/actuator/health"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.status").isEqualTo("UP");
    }

    @Test
    void onlyHealthIsExposed() {
        assertThat(mvc.get().uri("/actuator/env")).hasStatus(404);
        assertThat(mvc.get().uri("/actuator/beans")).hasStatus(404);
    }
}