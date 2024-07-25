package priam.data.priamdataservice;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import priam.data.priamdataservice.repositories.DataRepository;

@SpringBootApplication
@Configuration
@EnableFeignClients
public class PriamDataServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PriamDataServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner start(DataRepository dataRepository) {
        return args -> {
        };

    }
}
