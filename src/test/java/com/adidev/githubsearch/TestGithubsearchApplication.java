package com.adidev.githubsearch;

import org.springframework.boot.SpringApplication;

public class TestGithubsearchApplication {

	public static void main(String[] args) {
		SpringApplication.from(GithubsearchApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
