package com.paytmmoney.seats.config;

import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");

        if (databaseUrl != null && databaseUrl.startsWith("postgresql://")) {
            // Convert Render's DATABASE_URL format to JDBC format
            // Credentials are already embedded in the URL
            databaseUrl = "jdbc:" + databaseUrl;
            return DataSourceBuilder.create()
                    .url(databaseUrl)
                    .driverClassName("org.postgresql.Driver")
                    .build();
        } else {
            // Local development - use separate env vars or defaults
            return DataSourceBuilder.create()
                    .url("jdbc:postgresql://localhost:5433/seat_reservation?TimeZone=Asia/Kolkata")
                    .username(System.getenv().getOrDefault("DB_USERNAME", "postgres"))
                    .password(System.getenv().getOrDefault("DB_PASSWORD", "postgres"))
                    .driverClassName("org.postgresql.Driver")
                    .build();
        }
    }
}
