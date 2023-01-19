package com.marcura

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.utility.DockerImageName
import spock.lang.Specification

import java.nio.file.Paths

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BaseSpec extends Specification {

    @Autowired
    protected JdbcTemplate jdbcTemplate

    private static final Network NETWORK = Network.newNetwork()
    private static final def POSTGRESQL_PORT = 5432
    private static final def POSTGRES_ALIAS = "postgres"

    private static final def postgreSQLContainer = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:12"))
            .withNetwork(NETWORK)
            .withNetworkAliases(POSTGRES_ALIAS)
            .waitingFor(Wait.forListeningPort())

    private static final def dbMigration = new GenericContainer<>(
            new ImageFromDockerfile().withDockerfile(getPathToDbMigrationServiceDockerfile()))
            .withNetwork(NETWORK)
            .waitingFor(Wait.forLogMessage(".*Successfully applied.*", 1))

    def setupSpec() {
        postgreSQLContainer.start()
        configureAndStartDbMigrationContainer()
        configureSpringDataSource()
    }

    private static def getPathToDbMigrationServiceDockerfile() {
        return Paths.get("")
                    .toAbsolutePath()
                    .getParent()
                    .resolve("db-migration")
                    .resolve("Dockerfile")
    }

    private static def configureAndStartDbMigrationContainer() {
        dbMigration.addEnv("db.url", replacePortAndHostInPostgresJdbcUrl(
                postgreSQLContainer.getJdbcUrl()))
        dbMigration.addEnv("db.username", postgreSQLContainer.getUsername())
        dbMigration.addEnv("db.password", postgreSQLContainer.getPassword())
        dbMigration.start()
    }

    private static def replacePortAndHostInPostgresJdbcUrl(String url) {
        return url.replaceAll(":\\d+/", ":" + POSTGRESQL_PORT + "/")
                  .replaceAll("localhost", POSTGRES_ALIAS)
        // replace 172.17.0.1 and any derivatives with alias
                  .replaceAll("\\d+\\.\\d+\\.\\d+\\.\\d+", POSTGRES_ALIAS)
    }

    private static def configureSpringDataSource() {
        System.getProperties().putAll([
                "spring.datasource.url"     : postgreSQLContainer.getJdbcUrl(),
                "spring.datasource.username": postgreSQLContainer.getUsername(),
                "spring.datasource.password": postgreSQLContainer.getPassword()
        ])
    }
}
