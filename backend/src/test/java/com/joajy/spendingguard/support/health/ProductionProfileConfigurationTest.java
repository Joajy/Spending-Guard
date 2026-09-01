package com.joajy.spendingguard.support.health;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionProfileConfigurationTest {

    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @Test
    void productionProfileRequiresExternalConnectionsAndSecrets() throws IOException {
        PropertySource<?> source = load("application-prod.yml");

        assertThat(source.getProperty("spring.datasource.url")).isEqualTo("${DB_URL}");
        assertThat(source.getProperty("spring.datasource.username")).isEqualTo("${DB_USERNAME}");
        assertThat(source.getProperty("spring.datasource.password")).isEqualTo("${DB_PASSWORD}");
        assertThat(source.getProperty("spring.kafka.bootstrap-servers"))
                .isEqualTo("${KAFKA_BOOTSTRAP_SERVERS}");
        assertThat(source.getProperty("spending-guard.security.issuer")).isEqualTo("${JWT_ISSUER}");
        assertThat(source.getProperty("spending-guard.security.secret")).isEqualTo("${JWT_SECRET}");
        assertThat(source.getProperty("spending-guard.mail.from")).isEqualTo("${MAIL_FROM}");
    }

    @Test
    void healthGroupsSeparateProcessAndTrafficReadiness() throws IOException {
        PropertySource<?> source = load("application.yml");

        assertThat(source.getProperty("management.endpoint.health.probes.enabled")).isEqualTo(true);
        assertThat(source.getProperty("management.endpoint.health.probes.add-additional-paths"))
                .isEqualTo(true);
        assertThat(source.getProperty("management.endpoint.health.group.liveness.include"))
                .isEqualTo("livenessState");
        assertThat(source.getProperty("management.endpoint.health.group.readiness.include"))
                .isEqualTo("readinessState,db");
    }

    private PropertySource<?> load(String resource) throws IOException {
        return loader.load(resource, new ClassPathResource(resource)).getFirst();
    }
}
