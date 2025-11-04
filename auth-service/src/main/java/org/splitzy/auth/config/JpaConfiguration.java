package org.splitzy.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

// JPA configuration for entity auditing and transaction management
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.splitzy.auth.repository")
@EnableTransactionManagement
public class JpaConfiguration {
}

