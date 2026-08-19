package com.codesync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        exclude = {
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        }
)
public class CodeSyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeSyncApplication.class, args);
    }
}