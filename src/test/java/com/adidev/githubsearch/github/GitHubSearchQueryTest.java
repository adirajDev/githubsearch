package com.adidev.githubsearch.github;

import com.adidev.githubsearch.domain.SearchCriteria;
import com.adidev.githubsearch.domain.SortField;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubSearchQueryTest {

    @Test
    void restrictsMatchToRepositoryName() {
        var q = GitHubSearchQuery.from(new SearchCriteria("spring boot", null, SortField.STARS));
        assertThat(q.q()).isEqualTo("spring boot in:name");
    }

    @Test
    void addsLanguageQualifier() {
        var q = GitHubSearchQuery.from(new SearchCriteria("spring boot", "Java", SortField.STARS));
        assertThat(q.q()).isEqualTo("spring boot in:name language:Java");
    }

    @Test
    void quotesMultiWordLanguage() {
        var q = GitHubSearchQuery.from(new SearchCriteria("calc", "Visual Basic", SortField.STARS));
        assertThat(q.q()).isEqualTo("calc in:name language:\"Visual Basic\"");
    }

    @Test
    void ignoresBlankLanguageAndTrimsQuery() {
        var q = GitHubSearchQuery.from(new SearchCriteria("  spring  ", "  ", SortField.STARS));
        assertThat(q.q()).isEqualTo("spring in:name");
    }

    @Test
    void mapsSortAndUsesDescendingOrder() {
        var q = GitHubSearchQuery.from(new SearchCriteria("spring", null, SortField.UPDATED));
        assertThat(q.sort()).isEqualTo("updated");
        assertThat(q.order()).isEqualTo("desc");
        assertThat(q.perPage()).isEqualTo(GitHubSearchQuery.DEFAULT_PER_PAGE);
    }
}