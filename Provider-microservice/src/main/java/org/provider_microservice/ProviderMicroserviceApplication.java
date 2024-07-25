package org.provider_microservice;

import org.provider_microservice.repositories.ProviderViewRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class ProviderMicroserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProviderMicroserviceApplication.class, args);
    }

    @Bean
    CommandLineRunner start(ProviderViewRepository providerViewRepository) {
        return args -> {

        };

    }}
