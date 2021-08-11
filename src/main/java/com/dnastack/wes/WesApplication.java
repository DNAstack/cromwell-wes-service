package com.dnastack.wes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class WesApplication {

    public static void main(String[] args) {
        SpringApplication.run(WesApplication.class);
    }

}
