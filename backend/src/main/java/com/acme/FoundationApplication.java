package com.acme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.acme")
@EnableJpaRepositories(basePackages = "com.acme")
public class FoundationApplication {

    public static void main(String[] args) {
        // Liquibase changelog XSD is resolved over HTTP in this project setup.
        System.setProperty("liquibase.secureParsing", "false");
        SpringApplication.run(FoundationApplication.class, args);
    }
}
