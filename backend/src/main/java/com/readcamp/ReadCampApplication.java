package com.readcamp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.readcamp.mapper")
public class ReadCampApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReadCampApplication.class, args);
    }
}
