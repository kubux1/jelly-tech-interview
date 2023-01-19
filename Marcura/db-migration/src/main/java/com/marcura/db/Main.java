package com.marcura.db;

import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

@Component
@SpringBootApplication
public class Main implements CommandLineRunner {
    private final DatabaseConfiguration conf;

    public Main(final DatabaseConfiguration conf) {
        this.conf = conf;
    }

    public static void main(final String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Override
    public void run(final String... args) {
        Flyway.configure()
              .dataSource(conf.getUrl(), conf.getUsername(), conf.getPassword())
              .locations("db/migration")
              .load()
              .migrate();
    }
}
