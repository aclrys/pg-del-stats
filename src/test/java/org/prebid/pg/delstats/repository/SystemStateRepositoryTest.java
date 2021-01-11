package org.prebid.pg.delstats.repository;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class SystemStateRepositoryTest {
    SoftAssertions softAssertions;

    @Autowired
    SystemStateRepository systemStateRepository;

    @BeforeEach
    public void init() {
        softAssertions = new SoftAssertions();
    }

    @Test
    @Transactional
    public void shouldStoreAndRetrieveValueByTag() {
        softAssertions.assertThatCode(() -> systemStateRepository.store("test", "val"))
                .doesNotThrowAnyException();
        softAssertions.assertThat(systemStateRepository.retrieveByTag("test"))
                .hasFieldOrPropertyWithValue("val", "val");
        softAssertions.assertAll();
    }
}