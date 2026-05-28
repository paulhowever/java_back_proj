package ru.tischenko.vk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableCaching
@EnableJpaRepositories(basePackages = "ru.tischenko.vk.repository")
public class TeamCoordApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeamCoordApplication.class, args);
    }
}
