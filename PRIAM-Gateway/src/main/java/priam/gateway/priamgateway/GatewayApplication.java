package priam.gateway.priamgateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@Configuration
@CrossOrigin
@RestController
@PropertySource("classpath:custom.properties")
public class GatewayApplication {

    //Logger logger = LoggerFactory.getLogger(GatewayApplication.class);
    @Value("${custom.keycloak.url:http://localhost:8080}")
    private String keycloakURL;

    @Value("${custom.data.url:http://localhost:8081}")
    private String dataServiceURL;

    @Value("${custom.actor.url:http://localhost:8082}")
    private String actorServiceURL;

    @Value("${custom.right.url:http://localhost:8083}")
    private String rightServiceURL;

    @Value("${custom.provider.url:http://localhost:8086}")
    private String providerServiceURL;

    @Value("${custom.eureka.url:http://localhost:8761}")
    private String eurekaURL;

    @Value("${custom.cdp.url:http://localhost:8089}")
    private String cdpURL;

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public RouteLocator myRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(p -> p
                .path("/health")
                // Le moyen le plus simple pour vérifier si il marche
                // TODO: Rendre local le healthcheck
                .uri("https://http.cat/status/418")
                )
                .route(p -> p
                .path("/data/**")
                .filters(f -> f.rewritePath("/data/(?<segment>.*)", "/${segment}"))
                .uri(dataServiceURL)
                )
                .route(p -> p
                .path("/right/**")
                .filters(f -> f.rewritePath("/right/(?<segment>.*)", "/${segment}"))
                .uri(rightServiceURL)
                )
                .route(p -> p
                .path("/cdp/**")
                .filters(f -> f.rewritePath("/cdp/(?<segment>.*)", "/${segment}"))
                .uri(cdpURL)
                )
                .route(p -> p
                .path("/actor/**")
                .filters(f -> f.rewritePath("/actor/(?<segment>.*)", "/${segment}"))
                .uri(actorServiceURL)
                )
                .route(p -> p
                .path("/provider/**")
                .filters(f -> f.rewritePath("/provider/(?<segment>.*)", "/${segment}"))
                .uri(providerServiceURL)
                )
                .route(p -> p
                .path("/eureka/**")
                .filters(f -> f.rewritePath("/eureka/(?<segment>.*)", "/${segment}"))
                .uri(eurekaURL)
                )
                .route(p -> p
                .path("/**")
                .uri(keycloakURL)
                )
                .build();
    }

}
