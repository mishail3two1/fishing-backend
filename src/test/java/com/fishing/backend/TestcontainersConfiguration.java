package com.fishing.backend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    // Тот же образ, что в docker-compose: обычный postgres не умеет PostGIS.
    // asCompatibleSubstituteFor говорит Testcontainers, что это совместимая замена образа postgres.
    private static final DockerImageName POSTGIS_IMAGE = DockerImageName.parse("postgis/postgis:16-3.4")
            .asCompatibleSubstituteFor("postgres");

    // @ServiceConnection сам передаёт Spring URL, логин и пароль запущенного контейнера
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgisContainer() {
        return new PostgreSQLContainer(POSTGIS_IMAGE);
    }
}
