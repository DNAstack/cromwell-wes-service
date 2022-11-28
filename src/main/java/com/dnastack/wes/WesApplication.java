package com.dnastack.wes;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.dnastack")
public class WesApplication {

    public static void main(String[] args) {
        SpringApplication.run(WesApplication.class, args);
    }

}
