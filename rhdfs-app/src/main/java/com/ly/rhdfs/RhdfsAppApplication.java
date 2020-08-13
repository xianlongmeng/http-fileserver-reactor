package com.ly.rhdfs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(scanBasePackages = "com.ly.rhdfs")
public class RhdfsAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(RhdfsAppApplication.class, args);
    }

}
