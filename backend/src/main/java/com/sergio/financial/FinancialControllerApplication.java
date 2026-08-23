package com.sergio.financial;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FinancialControllerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinancialControllerApplication.class, args);
    }
}
