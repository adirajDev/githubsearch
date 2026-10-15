package com.adidev.githubsearch.repository;

import com.adidev.githubsearch.domain.GitHubRepo;
import com.adidev.githubsearch.domain.RepositoryFilter;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GitHubRepoSpecifications {

    private GitHubRepoSpecifications() { }

    public static Specification<GitHubRepo> matching(RepositoryFilter filter) {
        List<Specification<GitHubRepo>> specs = new ArrayList<>();
        if (filter.language() != null) {
            specs.add(languageIgnoringCase(filter.language()));
        }
        if (filter.minStars() != null) {
            specs.add(starsAtLeast(filter.minStars()));
        }
        return Specification.allOf(specs);
    }

    static Specification<GitHubRepo> languageIgnoringCase(String language) {
        String normalized = language.toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(cb.lower(root.<String>get("language")), normalized);
    }

    static Specification<GitHubRepo> starsAtLeast(int minStars) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<Integer>get("stars"), minStars);
    }
}