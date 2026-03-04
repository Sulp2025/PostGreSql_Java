package com.jt.summary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SummaryApplication {
    public static void main(String[] args) {
        SpringApplication.run(SummaryApplication.class, args);
    }
}
