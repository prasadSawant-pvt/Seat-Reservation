package com.paytmmoney.seats.config;

import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");

        if (databaseUrl != null && databaseUrl.startsWith("postgresql://")) {
            // Parse Render's DATABASE_URL format: postgresql://user:password@host:port/database or postgresql://user:password@host/database
            Pattern pattern = Pattern.compile("postgresql://([^:]+):([^@]+)@([^:/]+)(?::(\\d+))?/(.+)");
            Matcher matcher = pattern.matcher(databaseUrl);

            if (matcher.find()) {
                String username = matcher.group(1);
                String password = matcher.group(2);
                String host = matcher.group(3);
                String port = matcher.group(4); // May be null if port not specified
                String dbName = matcher.group(5);

                // Use default PostgreSQL port 5432 if not specified
                String portStr = (port != null && !port.isEmpty()) ? port : "5432";
                String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s?TimeZone=Asia/Kolkata", host, portStr, dbName);

                return DataSourceBuilder.create()
                        .url(jdbcUrl)
                        .username(username)
                        .password(password)
                        .driverClassName("org.postgresql.Driver")
                        .build();
            }
        }

        // Local development - use separate env vars or defaults
        return DataSourceBuilder.create()
                .url("jdbc:postgresql://localhost:5433/seat_reservation?TimeZone=Asia/Kolkata")
                .username(System.getenv().getOrDefault("DB_USERNAME", "postgres"))
                .password(System.getenv().getOrDefault("DB_PASSWORD", "postgres"))
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}
