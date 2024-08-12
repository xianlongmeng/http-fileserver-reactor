package com.ly.hdfs.master;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.ly.common","com.ly.rhdfs"})
public class HdfsMasterAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(HdfsMasterAppApplication.class, args);
    }

}
