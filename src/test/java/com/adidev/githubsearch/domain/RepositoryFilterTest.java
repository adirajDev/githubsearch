package com.adidev.githubsearch.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepositoryFilterTest {

    @Test
    void rejectsNegativeMinStars() {
        assertThatThrownBy(() -> new RepositoryFilter(null, -1, null, 0, 30))
                .isInstanceOfSatisfying(InvalidParameterException.class,
                        e -> assertThat(e.getParameter()).isEqualTo("minStars"));
    }

    @Test
    void rejectsNegativePage() {
        assertThatThrownBy(() -> new RepositoryFilter(null, null, null, -1, 30))
                .isInstanceOfSatisfying(InvalidParameterException.class,
                        e -> assertThat(e.getParameter()).isEqualTo("page"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101})
    void rejectsSizeOutsideRange(int size) {
        assertThatThrownBy(() -> new RepositoryFilter(null, null, null, 0, size))
                .isInstanceOfSatisfying(InvalidParameterException.class,
                        e -> assertThat(e.getParameter()).isEqualTo("size"));
    }

    @Test
    void normalisesBlankLanguageAndDefaultsSort() {
        var filter = new RepositoryFilter("  ", null, null, 0, 30);
        assertThat(filter.language()).isNull();
        assertThat(filter.sort()).isEqualTo(SortField.STARS);
    }
}