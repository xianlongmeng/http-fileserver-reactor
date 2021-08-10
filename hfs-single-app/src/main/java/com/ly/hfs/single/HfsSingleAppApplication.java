package com.ly.hfs.single;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.ly.common","com.ly.rhdfs"})
public class HfsSingleAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(HfsSingleAppApplication.class, args);
    }

}
