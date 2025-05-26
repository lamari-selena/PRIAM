package priam.actor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import priam.actor.repositories.DataSubjectCategoryRepository;
import priam.actor.repositories.DataSubjectRepository;
//import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@Configuration
//@EnableEurekaClient
public class PriamActorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PriamActorServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner start(DataSubjectRepository dataSubjectRepository, DataSubjectCategoryRepository dataSubjectCategoryRepository) {
        return args -> {

        };
    }
}
