package com.adidev.githubsearch.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SortFieldTest {

    @ParameterizedTest
    @CsvSource({"stars,STARS", "FORKS,FORKS", " Updated ,UPDATED"})
    void parsesCaseInsensitively(String raw, SortField expected) {
        assertThat(SortField.fromValue(raw)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void defaultsToStarsWhenMissing(String raw) {
        assertThat(SortField.fromValue(raw)).isEqualTo(SortField.STARS);
    }

    @Test
    void rejectsUnknownValueWithHelpfulMessage() {
        assertThatThrownBy(() -> SortField.fromValue("popularity"))
                .isInstanceOfSatisfying(InvalidParameterException.class, e -> {
                    assertThat(e.getParameter()).isEqualTo("sort");
                    assertThat(e.getMessage()).contains("stars, forks, updated");
                });
    }
}