package com.adidev.githubsearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GithubsearchApplication {

	public static void main(String[] args) {
		SpringApplication.run(GithubsearchApplication.class, args);
	}

}
