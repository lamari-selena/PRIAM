package priam.consent.priamconsentservice;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import priam.consent.priamconsentservice.repositories.ConsentRepository;
import priam.consent.priamconsentservice.repositories.ContractRepository;


@Configuration
@EnableFeignClients
@SpringBootApplication
public class PriamConsentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PriamConsentServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner start(ConsentRepository consentRepository, ContractRepository contractRepository) {
        return args -> {

        };

    }
}
