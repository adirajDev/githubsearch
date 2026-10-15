package com.adidev.githubsearch.web;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Sends visitors of the bare URL to the API docs; this service has no UI of its own. */
@Hidden
@Controller
class RootController {

    @GetMapping("/")
    String root() {
        return "redirect:/swagger-ui/index.html";
    }
}