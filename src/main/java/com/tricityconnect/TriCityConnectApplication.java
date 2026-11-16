package com.tricityconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class TriCityConnectApplication {

    public static void main(String[] args) {
        // All bus timings are Indian Standard Time
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(TriCityConnectApplication.class, args);
    }
}
