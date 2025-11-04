package org.splitzy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

// Handles user authentication, JWT token generation, and Redis-based token blacklisting
@SpringBootApplication
@ComponentScan(basePackages = {
        "org.splitzy.auth",
        "org.splitzy.common"  // Include common package for shared components
})
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}