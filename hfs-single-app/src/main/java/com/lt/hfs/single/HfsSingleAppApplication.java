package com.lt.hfs.single;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.ly"})
public class HfsSingleAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(HfsSingleAppApplication.class, args);
    }

}
