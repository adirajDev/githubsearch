package com.adidev.githubsearch.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchCriteriaTest {

    @Test
    void rejectsBlankQuery() {
        assertThatThrownBy(() -> new SearchCriteria("  ", "Java", SortField.STARS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defaultsSortToStars() {
        assertThat(new SearchCriteria("spring", null, null).sort()).isEqualTo(SortField.STARS);
    }
}