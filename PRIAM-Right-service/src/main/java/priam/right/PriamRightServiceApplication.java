package priam.right;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import priam.right.services.DataRequestService;

@SpringBootApplication
@Configuration
@EnableFeignClients
public class PriamRightServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PriamRightServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(DataRequestService dataRequestService) {
        return args -> {
           };
    }
}

