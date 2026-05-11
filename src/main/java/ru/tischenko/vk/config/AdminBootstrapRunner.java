package ru.tischenko.vk.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.tischenko.vk.domain.Enums.Role;
import ru.tischenko.vk.domain.Enums.UserLevel;
import ru.tischenko.vk.domain.UserEntity;
import ru.tischenko.vk.repository.Repositories.UserRepository;

@Configuration
public class AdminBootstrapRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    @Bean
    ApplicationRunner seedAdmin(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${app.bootstrap.admin-email:admin@local}") String email,
                                @Value("${app.bootstrap.admin-password:admin12345}") String password) {
        return args -> {
            if (userRepository.findByEmail(email).isPresent()) {
                return;
            }
            UserEntity admin = new UserEntity();
            admin.setEmail(email);
            admin.setPasswordHash(passwordEncoder.encode(password));
            admin.setRole(Role.ADMIN);
            admin.setLevel(UserLevel.LEAD);
            userRepository.save(admin);
            log.info("Bootstrapped default ADMIN user '{}' (please change password)", email);
        };
    }
}
