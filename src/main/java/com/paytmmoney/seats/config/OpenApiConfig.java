package com.paytmmoney.seats.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI seatReservationOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Seat Reservation API")
                        .description("Seat Reservation at Scale handling 20k Requests")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Paytm Money")
                                .url("https://paytmmoney.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development"),
                        new Server().url("https://seat-reservation-dfr6.onrender.com").description("Production")
                ));
    }
}
