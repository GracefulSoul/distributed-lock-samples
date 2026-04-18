package com.gracefulsoul.lock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DistributedLockSamplesApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedLockSamplesApplication.class, args);
    }

}
