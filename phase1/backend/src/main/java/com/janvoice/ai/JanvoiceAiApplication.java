package com.janvoice.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application entrypoint runner class.
 *
 * The @SpringBootApplication annotation is a convenience annotation that adds all of the following:
 * 1. @Configuration: Tags the class as a source of bean definitions.
 * 2. @EnableAutoConfiguration: Tells Spring Boot to start adding beans based on classpath settings, other beans, and property settings.
 * 3. @ComponentScan: Tells Spring to look for other components, configurations, and services in the com.janvoice.ai package.
 */
@SpringBootApplication
public class JanvoiceAiApplication {

    public static void main(String[] args) {
        // Runs the Spring Boot framework container environment
        SpringApplication.run(JanvoiceAiApplication.class, args);
    }
}
