package org.prebid.pg.delstats.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories("org.prebid.pg.delstats.repository")
@EnableTransactionManagement
public class PersistenceConfiguration {
}
